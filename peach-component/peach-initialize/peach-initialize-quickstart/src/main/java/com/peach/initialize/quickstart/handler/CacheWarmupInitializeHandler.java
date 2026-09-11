package com.peach.initialize.quickstart.handler;

import com.peach.initialize.base.InitializeHandler;
import com.peach.initialize.constant.InitializeHandlerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 启动预热处理器：在 ApplicationStartedEvent 阶段执行一次缓存预热演示。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
public class CacheWarmupInitializeHandler implements InitializeHandler {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupInitializeHandler.class);

    private final AtomicBoolean warmedUp = new AtomicBoolean(false);
    private final AtomicReference<String> warmupPayload = new AtomicReference<>();

    @Override
    public String type() {
        return InitializeHandlerType.APP_EVENT_LISTENER;
    }

    @Override
    public Integer executeOrder() {
        return 100;
    }

    @Override
    public void executeInitialize(ConfigurableApplicationContext context) {
        warmupPayload.set("cache-ready");
        warmedUp.set(true);
        log.info("initialize quickstart warmup finished, beanCount={}", context.getBeanDefinitionCount());
    }

    /**
     * @return 是否已完成预热
     */
    public boolean isWarmedUp() {
        return warmedUp.get();
    }

    /**
     * @return 预热结果摘要
     */
    public String getWarmupPayload() {
        return warmupPayload.get();
    }
}
