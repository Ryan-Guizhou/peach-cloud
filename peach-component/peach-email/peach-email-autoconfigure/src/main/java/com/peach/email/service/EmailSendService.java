package com.peach.email.service;

import com.peach.common.util.StringUtil;
import com.peach.email.Idempotency.IdempotencyStore;
import com.peach.email.constant.EmailConstant;
import com.peach.email.core.EmailContext;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.EmailTransport;
import com.peach.email.core.SendResult;
import com.peach.email.retry.RetryPolicy;
import com.peach.email.router.ProviderRouter;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 17:50
 */
public class EmailSendService {

    private final ProviderRouter router;

    private final IdempotencyStore idempotencyStore;

    private final RetryPolicy retryPolicy;

    private final String defaultProvider;

    public EmailSendService(ProviderRouter router, IdempotencyStore idempotencyStore, RetryPolicy retryPolicy, String defaultProvider) {
        this.router = router;
        this.idempotencyStore = idempotencyStore;
        this.retryPolicy = retryPolicy;
        this.defaultProvider = defaultProvider;
    }

    public SendResult send(String providerName,EmailMessage message) {
        SendResult result = router.send(providerName, message);
        return result;
    }


    /**
     * 使用默认提供商与优先级进行故障转移与重试
     */
    public SendResult sendAuto(EmailMessage message) {
        // 1、 判断是否已经提交过，如果提交直接返回成功
        String idempotencyKey = message.getIdempotencyKey();
        if (idempotencyKey != null && idempotencyStore.exists(idempotencyKey)){
            return new SendResult(defaultProvider, null, EmailConstant.DEFAULT_DURATION_MILLIS, true, null);
        }

        List<String> candidates = new ArrayList<String>(router.getProviderNames());
        if (defaultProvider != null && candidates.contains(defaultProvider)) {
            candidates.remove(defaultProvider);
            candidates.add(0, defaultProvider);
        }
        List<String> ordered = router.orderByPriority(candidates);
        List<String> errors = new ArrayList<String>();
        long start = System.currentTimeMillis();
        for (String name : ordered) {
            EmailTransport t = router.getTransport(name);
            EmailContext c = router.getContext(name);
            if (t == null || c == null){
                continue;
            }
            SendResult r = sendWithRetry(t, c, message);
            if (r != null && r.isSuccess()) {
                if (idempotencyStore != null) {
                    idempotencyStore.record(message.getIdempotencyKey(), r);
                }
                return new SendResult(name, r.getMessageId(), System.currentTimeMillis() - start, true, null);
            } else if (r != null) {
                errors.add(name + StringUtil.SEPARATOR_COLON + r.getError());
            }
        }
        return new SendResult(defaultProvider, null, System.currentTimeMillis() - start, false, errors.toString());
    }


    /**
     * 执行发送并按策略进行重试
     */
    private SendResult sendWithRetry(EmailTransport t, EmailContext c, EmailMessage m) {
        if (retryPolicy == null) {
            return t.send(m, c);
        }
        int attempts = 0;
        Throwable last = null;
        while (attempts < retryPolicy.getMaxAttempts()) {
            attempts ++;
            SendResult r = t.send(m, c);
            if (r.isSuccess()) return r;
            last = new RuntimeException(r.getError());
            if (!retryPolicy.isRetryable(last)) {
                break;
            }
            try {
                Thread.sleep(retryPolicy.computeDelayMillis(attempts));
            } catch (InterruptedException ignored) {
                throw new RuntimeException("sendWithRetry Interrupted");
            }
        }
        return new SendResult(t.getName(), null, EmailConstant.DEFAULT_DURATION_MILLIS,
                false, last != null ? last.getMessage() : EmailConstant.DEFAULT_FAILED_MESSAGE);
    }

}
