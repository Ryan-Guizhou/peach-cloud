package com.peach.email.service;

import com.peach.email.idempotency.IdempotencyStore;
import com.peach.email.idempotency.ClaimingIdempotencyStore;
import com.peach.email.constant.EmailConstant;
import com.peach.email.core.EmailContext;
import com.peach.email.core.EmailFailureKind;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.EmailTransport;
import com.peach.email.core.SendResult;
import com.peach.email.retry.RetryPolicy;
import com.peach.email.router.ProviderRouter;

import jakarta.mail.MessagingException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.List;

/**
 * 邮件发送服务：负责根据配置的提供商路由发送邮件，。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 17:50
 * @Description 邮件发送服务：负责根据配置的提供商路由发送邮件，
 */
public class EmailSendService {

    private final ProviderRouter router;

    private final IdempotencyStore idempotencyStore;

    private final RetryPolicy retryPolicy;

    private final String defaultProvider;

    private static final int MAX_CONCURRENT_AUTO_SENDS = 256;

    private final Semaphore sendPermits = new Semaphore(MAX_CONCURRENT_AUTO_SENDS);

    /** 本实例用于兼容旧 IdempotencyStore 的本地同键请求拦截；跨节点需 ClaimingIdempotencyStore。 */
    private final ConcurrentMap<String, Object> inFlightKeys = new ConcurrentHashMap<>();

    public EmailSendService(ProviderRouter router, IdempotencyStore idempotencyStore, RetryPolicy retryPolicy, String defaultProvider) {
        this.router = router;
        this.idempotencyStore = idempotencyStore;
        this.retryPolicy = retryPolicy;
        this.defaultProvider = defaultProvider;
    }

    public SendResult send(String providerName,EmailMessage message) {
        return router.send(providerName, message);
    }


    /**
     * 对同一幂等键先认领再调用外部 SMTP，避免本进程内并发重复发送。
     * 使用支持认领的存储时允许跨节点原子占用；旧存储仅保留单实例保护。
     *
     * @param message 邮件请求
     * @return 成功结果或明确失败/结果未知
     */
    public SendResult sendAuto(EmailMessage message) {
        Objects.requireNonNull(message, "message");
        if (!sendPermits.tryAcquire()) {
            return new SendResult(defaultProvider, null, 0L, false,
                    "Email send concurrency limit reached",
                    EmailFailureKind.PERMANENT_BEFORE_SEND);
        }
        String key = message.getIdempotencyKey();
        if (key != null && key.isBlank()) {
            key = null;
        }
        Object localToken = new Object();
        boolean ownsLocalKey = false;
        try {
            if (key != null) {
                if (inFlightKeys.putIfAbsent(key, localToken) != null) {
                    return new SendResult(defaultProvider, null, 0L, false,
                            "Email idempotency key is in flight", EmailFailureKind.DELIVERY_UNKNOWN);
                }
                ownsLocalKey = true;
            }
            if (key != null && idempotencyStore instanceof ClaimingIdempotencyStore claims) {
                return sendWithClaim(message, key, claims);
            }
            // 兼容历史 IdempotencyStore；同进程同键互斥，但不承诺跨实例原子性。
            if (key != null && idempotencyStore != null && idempotencyStore.exists(key)) {
                return alreadySent();
            }
            SendResult result = routeAndSend(message);
            if (result.isSuccess() && key != null && idempotencyStore != null) {
                idempotencyStore.storeSendResult(key, result);
            }
            return result;
        } finally {
            if (ownsLocalKey) {
                inFlightKeys.remove(key, localToken);
            }
            sendPermits.release();
        }
    }

    private SendResult sendWithClaim(EmailMessage message, String key, ClaimingIdempotencyStore claims) {
        String claimToken = claims.tryClaim(key);
        if (claimToken == null) {
            return claims.exists(key) ? alreadySent()
                    : new SendResult(defaultProvider, null, 0L, false,
                    "Email idempotency key is in flight or delivery is unresolved",
                    EmailFailureKind.DELIVERY_UNKNOWN);
        }
        boolean completed = false;
        try {
            SendResult result = routeAndSend(message);
            if (result.isSuccess()) {
                claims.markSent(key, claimToken, result);
            } else if (result.getFailureKind() == EmailFailureKind.DELIVERY_UNKNOWN) {
                claims.markUnknown(key, claimToken);
            } else {
                claims.releaseClaim(key, claimToken);
            }
            completed = true;
            return result;
        } finally {
            if (!completed) {
                // 任何未能完成状态提交的异常都按结果未知保护，避免自动二次发送。
                claims.markUnknown(key, claimToken);
            }
        }
    }

    private SendResult alreadySent() {
        return new SendResult(defaultProvider, null,
                EmailConstant.DEFAULT_DURATION_MILLIS, true, null);
    }

    /**
     * 仅连接阶段明确未发送的临时故障可继续尝试下一个 Provider。
     */
    private SendResult routeAndSend(EmailMessage message) {
        List<String> candidates = new ArrayList<>(router.getProviderNames());
        if (defaultProvider != null && candidates.remove(defaultProvider)) {
            candidates.add(0, defaultProvider);
        }
        List<String> ordered = router.orderByPriority(candidates);
        long start = System.currentTimeMillis();
        for (String name : ordered) {
            EmailTransport transport = router.getTransport(name);
            EmailContext context = router.getContext(name);
            if (transport == null || context == null) {
                continue;
            }
            SendResult result = sendWithRetry(transport, context, message);
            if (result.isSuccess()) {
                return new SendResult(name, result.getMessageId(),
                        System.currentTimeMillis() - start, true, null);
            }
            if (result.getFailureKind() != EmailFailureKind.RETRYABLE_BEFORE_SEND) {
                return new SendResult(name, null, System.currentTimeMillis() - start, false,
                        "SMTP delivery not confirmed; no automatic retry", result.getFailureKind());
            }
        }
        return new SendResult(defaultProvider, null, System.currentTimeMillis() - start,
                false, "No SMTP provider could deliver the message before submission",
                EmailFailureKind.RETRYABLE_BEFORE_SEND);
    }

    /**
     * 仅在明确未提交邮件的可重试故障后重试，最大退避时间为五分钟。
     *
     * @param transport 实际发送 Provider
     * @param context SMTP 连接参数
     * @param message 邮件
     * @return 发送结果
     */
    private SendResult sendWithRetry(EmailTransport transport, EmailContext context, EmailMessage message) {
        int maximumAttempts = retryPolicy == null ? 1 : Math.max(1, retryPolicy.getMaxAttempts());
        for (int attempt = 1; attempt <= maximumAttempts; attempt++) {
            SendResult result;
            try {
                result = transport.send(message, context);
            } catch (RuntimeException ex) {
                // 第三方 Transport 未返回阶段信息时不能推断邮件未发送。
                return new SendResult(transport.getName(), null, EmailConstant.DEFAULT_DURATION_MILLIS,
                        false, ex.getClass().getSimpleName(), EmailFailureKind.DELIVERY_UNKNOWN);
            }
            if (result == null) {
                return new SendResult(transport.getName(), null, EmailConstant.DEFAULT_DURATION_MILLIS,
                        false, "Null SMTP result", EmailFailureKind.DELIVERY_UNKNOWN);
            }
            if (result.isSuccess() || result.getFailureKind() != EmailFailureKind.RETRYABLE_BEFORE_SEND) {
                return result;
            }
            if (retryPolicy == null || attempt >= maximumAttempts
                    || !retryPolicy.isRetryable(new MessagingException("SMTP connection failed before send"))) {
                return result;
            }
            long delay = Math.clamp(retryPolicy.computeDelayMillis(attempt), 0L, 300_000L);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("SMTP retry interrupted", ex);
            }
        }
        throw new IllegalStateException("SMTP retry control flow exhausted");
    }
}
