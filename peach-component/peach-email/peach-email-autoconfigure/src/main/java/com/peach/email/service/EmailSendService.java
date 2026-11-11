package com.peach.email.service;

import com.peach.email.idempotency.IdempotencyStore;
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

    private static final String SEPARATOR_COLON = ":";

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
     * 自动发送优先遵循投递确定性：只有明确未提交的临时故障才允许换 Provider。
     * SMTP 已开始发送后的失败视为未知，不继续自动重复发送。
     *
     * @param message 邮件请求
     * @return 本次确定成功或失败/未知状态
     */
    public SendResult sendAuto(EmailMessage message) {
        Objects.requireNonNull(message, "message");
        String idempotencyKey = message.getIdempotencyKey();
        if (idempotencyKey != null && idempotencyStore != null
                && idempotencyStore.exists(idempotencyKey)) {
            return new SendResult(defaultProvider, null, EmailConstant.DEFAULT_DURATION_MILLIS, true, null);
        }

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
                if (idempotencyStore != null && idempotencyKey != null) {
                    idempotencyStore.storeSendResult(idempotencyKey, result);
                }
                return new SendResult(name, result.getMessageId(),
                        System.currentTimeMillis() - start, true, null);
            }
            // SMTP DATA 已尝试送达时，跨 Provider 故障转移会造成外部副作用重复。
            if (result.getFailureKind() != EmailFailureKind.RETRYABLE_BEFORE_SEND) {
                return new SendResult(name, null, System.currentTimeMillis() - start,
                        false, "SMTP delivery not confirmed; no automatic retry",
                        result.getFailureKind());
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
