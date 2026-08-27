package com.peach.email.idempotency;

import com.peach.email.core.SendResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:19
 * @Description 简单幂等存储实现
 */
@Slf4j
public class SimpleIdempotencyStore implements IdempotencyStore{

    private final ConcurrentHashMap<String, SendResult> store = new ConcurrentHashMap<>();

    public SimpleIdempotencyStore() {
      log.info("Init SimpleIdempotencyStore successful");
    }

    @Override
    public boolean exists(String key) {
        return Optional.ofNullable(key)
                .filter(StringUtils::isNotBlank)
                .map(store::containsKey)
                .orElse(false);
    }

    @Override
    public void storeSendResult(String key, SendResult result) {
        Optional.ofNullable(key)
                .flatMap(k -> Optional.ofNullable(result))
                .filter(SendResult::isSuccess)
                .ifPresent(r -> store.put(key, r));
    }
}
