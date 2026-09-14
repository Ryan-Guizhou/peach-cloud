package com.peach.initialize.quickstart.example;

import com.peach.initialize.quickstart.handler.CacheWarmupInitializeHandler;
import com.peach.initialize.quickstart.handler.ResourceCheckInitializeHandler;
import com.peach.initialize.quickstart.status.InitializeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 查询同类型 Handler 按 {@code executeOrder} 升序执行后的实际顺序。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:20
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class HandlerOrderExample {

    private final InitializeStatus initializeStatus;

    private final CacheWarmupInitializeHandler cacheWarmupInitializeHandler;

    private final ResourceCheckInitializeHandler resourceCheckInitializeHandler;

    /**
     * @return 业务 Handler 的实际执行顺序
     */
    public List<String> executedHandlers() {
        return initializeStatus.executedHandlers();
    }

    /**
     * @return 预热 Handler 的编排顺序
     */
    public int warmupOrder() {
        return cacheWarmupInitializeHandler.executeOrder();
    }

    /**
     * @return 资源检查 Handler 的编排顺序
     */
    public int resourceCheckOrder() {
        return resourceCheckInitializeHandler.executeOrder();
    }
}
