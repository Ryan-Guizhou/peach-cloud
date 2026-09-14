package com.peach.initialize.quickstart.handler;

import com.peach.initialize.base.AbstractAppStartedEventHandler;
import com.peach.initialize.quickstart.status.InitializeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 自定义启动预热 Handler：在 {@code ApplicationStartedEvent} 阶段写入成功标记。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:20
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
public class CacheWarmupInitializeHandler extends AbstractAppStartedEventHandler {

    private final InitializeStatus initializeStatus;

    @Override
    public Integer executeOrder() {
        return 10;
    }

    @Override
    public void executeInitialize(ConfigurableApplicationContext context) {
        initializeStatus.record(InitializeStatus.HANDLER_CACHE_WARMUP);
        initializeStatus.markWarmedUp(InitializeStatus.WARMUP_PAYLOAD);
        log.info("cache warmup finished, beanCount={}", context.getBeanDefinitionCount());
    }
}
