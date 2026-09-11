package com.peach.redis.stream.quickstart.runner;

import com.peach.redis.stream.quickstart.example.OrderStreamExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示 Stream push/consume 与 RecordId 幂等。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.stream.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisStreamDemoRunner implements ApplicationRunner {

    private final OrderStreamExample orderStreamExample;

    @Override
    public void run(ApplicationArguments args) {
        log.info("=== Stream push + consume demo ===");
        String consumed = orderStreamExample.pushAndConsume("ord-demo-1", "SHIPPED");
        log.info("consumed={}", consumed);

        log.info("=== Stream idempotent replay demo ===");
        orderStreamExample.pushThenReplayDuplicate("ord-demo-dup", "PAID");
        log.info("redis-stream demo finished");
    }
}
