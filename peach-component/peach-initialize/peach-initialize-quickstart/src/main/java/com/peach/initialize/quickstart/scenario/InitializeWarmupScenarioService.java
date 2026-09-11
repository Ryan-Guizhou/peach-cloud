package com.peach.initialize.quickstart.scenario;

import com.peach.initialize.quickstart.handler.CacheWarmupInitializeHandler;
import org.springframework.stereotype.Service;

/**
 * 启动预热场景查询入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class InitializeWarmupScenarioService {

    private final CacheWarmupInitializeHandler warmupInitializeHandler;

    /**
     * @param warmupInitializeHandler 预热处理器
     */
    public InitializeWarmupScenarioService(CacheWarmupInitializeHandler warmupInitializeHandler) {
        this.warmupInitializeHandler = warmupInitializeHandler;
    }

    /**
     * @return 预热是否完成
     */
    public boolean isWarmedUp() {
        return warmupInitializeHandler.isWarmedUp();
    }

    /**
     * @return 预热结果摘要
     */
    public String getWarmupPayload() {
        return warmupInitializeHandler.getWarmupPayload();
    }
}
