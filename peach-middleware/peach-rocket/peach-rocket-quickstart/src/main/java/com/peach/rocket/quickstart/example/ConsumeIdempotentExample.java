package com.peach.rocket.quickstart.example;

import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentClaim;
import com.peach.rocket.idempotent.MqIdempotentStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 消费幂等：同一事件 ID 获得认领令牌并确认后，后续认领应返回已完成。
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
        MqIdempotentClaim first = idempotentStore.claim(context);
        if (first.status() == MqIdempotentClaim.Status.ACQUIRED) {
            processed++;
            idempotentStore.markSuccess(context, first);
        }
        MqIdempotentClaim second = idempotentStore.claim(context);
        if (second.status() == MqIdempotentClaim.Status.ACQUIRED) {
            processed++;
            idempotentStore.markSuccess(context, second);
        }
        log.info("[mq-quickstart] idempotent consumeTwice processed={} completed={}",
                processed, idempotentStore.claim(context).status() == MqIdempotentClaim.Status.SUCCEEDED);
        return processed;
    }
}
