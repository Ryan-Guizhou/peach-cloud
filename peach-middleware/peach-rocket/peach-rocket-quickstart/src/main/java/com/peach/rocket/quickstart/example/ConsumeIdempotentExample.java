package com.peach.rocket.quickstart.example;

import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 消费幂等：同一幂等键 {@code tryStart + markSuccess} 后再次 {@code tryStart} 应被拒绝。
 *
 * <p>默认 {@link com.peach.rocket.idempotent.InMemoryMqIdempotentStore} 只覆盖单进程，
 * 不能当作集群级 Exactly-Once。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ConsumeIdempotentExample {

    private static final Duration EXPIRE = Duration.ofHours(24);

    private final MqIdempotentStore idempotentStore;

    /**
     * 使用同一幂等键连续尝试两次消费。
     *
     * @param idempotentKey 稳定业务幂等键
     * @return 实际获得处理权的次数
     */
    public int consumeTwice(String idempotentKey) {
        MqIdempotentContext context = new MqIdempotentContext(
                idempotentKey,
                "peach-rocket-quickstart-order-created",
                "order",
                "created",
                idempotentKey,
                idempotentKey,
                EXPIRE);
        int processed = 0;
        if (idempotentStore.tryStart(context)) {
            processed++;
            idempotentStore.markSuccess(context);
        }
        if (idempotentStore.tryStart(context)) {
            processed++;
            idempotentStore.markSuccess(context);
        }
        log.info("idempotent consumeTwice, key={}, processed={}, success={}",
                idempotentKey, processed, idempotentStore.isSuccess(context));
        return processed;
    }
}
