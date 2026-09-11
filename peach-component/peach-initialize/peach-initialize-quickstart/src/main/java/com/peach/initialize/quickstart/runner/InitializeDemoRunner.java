package com.peach.initialize.quickstart.runner;

import com.peach.initialize.quickstart.example.HandlerOrderExample;
import com.peach.initialize.quickstart.example.WarmupStatusExample;
import com.peach.initialize.quickstart.status.InitializeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 启动后核验预热成功标记与同类型 Handler 的 {@code executeOrder}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:20
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.initialize.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InitializeDemoRunner implements ApplicationRunner {

    private final WarmupStatusExample warmupStatusExample;

    private final HandlerOrderExample handlerOrderExample;

    @Override
    public void run(ApplicationArguments args) {
        runWarmupDemo();
        runOrderDemo();
        log.info("initialize demo finished");
    }

    private void runWarmupDemo() {
        log.info("=== warmup status demo ===");
        boolean warmedUp = warmupStatusExample.isWarmedUp();
        String payload = warmupStatusExample.warmupPayload();
        log.info("warmup warmedUp={}, payload={}", warmedUp, payload);
        if (!warmedUp || !InitializeStatus.WARMUP_PAYLOAD.equals(payload)) {
            throw new IllegalStateException("initialize warmup demo failed");
        }
    }

    private void runOrderDemo() {
        log.info("=== executeOrder demo ===");
        List<String> executed = handlerOrderExample.executedHandlers();
        log.info("executedHandlers={}, warmupOrder={}, resourceCheckOrder={}",
                executed, handlerOrderExample.warmupOrder(), handlerOrderExample.resourceCheckOrder());
        if (!executed.equals(List.of(
                InitializeStatus.HANDLER_CACHE_WARMUP,
                InitializeStatus.HANDLER_RESOURCE_CHECK))) {
            throw new IllegalStateException("initialize executeOrder demo failed");
        }
    }
}
