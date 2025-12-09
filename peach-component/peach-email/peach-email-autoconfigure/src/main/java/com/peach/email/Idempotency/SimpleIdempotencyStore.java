package com.peach.email.Idempotency;

import com.peach.common.util.StringUtil;
import com.peach.email.core.SendResult;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:19
 */
public class SimpleIdempotencyStore implements IdempotencyStore{

    private final ConcurrentHashMap<String, SendResult> store = new ConcurrentHashMap<String, SendResult>();

    @Override
    public boolean exists(String key) {
        return Optional.ofNullable(key)
                .filter(StringUtil::isNotBlank)
                .map(store::containsKey)
                .orElse(false);
    }

    @Override
    public void record(String key, SendResult result) {
        Optional.ofNullable(key)
                .flatMap(k -> Optional.ofNullable(result))
                .filter(SendResult::isSuccess)
                .ifPresent(r -> store.put(key, r));
    }
}
