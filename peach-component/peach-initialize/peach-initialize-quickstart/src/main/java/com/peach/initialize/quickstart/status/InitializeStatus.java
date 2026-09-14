package com.peach.initialize.quickstart.status;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 记录启动初始化的成功标记与同类型 Handler 实际执行顺序。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:20
 */
@Slf4j
@Indexed
@Component
public class InitializeStatus {

    public static final String HANDLER_CACHE_WARMUP = "cache-warmup";

    public static final String HANDLER_RESOURCE_CHECK = "resource-check";

    public static final String WARMUP_PAYLOAD = "cache-ready";

    private final AtomicBoolean warmedUp = new AtomicBoolean(false);

    private final AtomicReference<String> warmupPayload = new AtomicReference<>();

    private final CopyOnWriteArrayList<String> executedHandlers = new CopyOnWriteArrayList<>();

    /**
     * 记录一次 Handler 执行，顺序即编排结果。
     *
     * @param handlerName 处理器标识
     */
    public void record(String handlerName) {
        executedHandlers.add(handlerName);
        log.info("initialize handler recorded, name={}, sequence={}", handlerName, executedHandlers.size());
    }

    /**
     * 写入预热成功标记。
     *
     * @param payload 预热结果摘要
     */
    public void markWarmedUp(String payload) {
        warmupPayload.set(payload);
        warmedUp.set(true);
        log.info("initialize warmup marked, payload={}", payload);
    }

    public boolean isWarmedUp() {
        return warmedUp.get();
    }

    public String getWarmupPayload() {
        return warmupPayload.get();
    }

    /**
     * @return 已按实际执行顺序记录的 Handler 标识
     */
    public List<String> executedHandlers() {
        return List.copyOf(executedHandlers);
    }
}
