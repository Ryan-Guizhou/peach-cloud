package com.peach.initialize.quickstart.example;

import com.peach.initialize.quickstart.status.InitializeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 查询启动预热 Handler 写入的成功标记。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:20
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class WarmupStatusExample {

    private final InitializeStatus initializeStatus;

    /**
     * @return 预热是否已在启动编排中完成
     */
    public boolean isWarmedUp() {
        return initializeStatus.isWarmedUp();
    }

    /**
     * @return 预热结果摘要，未完成时为 {@code null}
     */
    public String warmupPayload() {
        return initializeStatus.getWarmupPayload();
    }
}
