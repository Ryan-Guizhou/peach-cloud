package com.peach.log.core;

import com.peach.log.vo.UserOperLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 每5秒异步刷库，每批最多200条；失败保留同一批次及稳定ID，下一轮幂等重试。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class UserOperLogTask {
    private final IUserOperLogService userOperLogService;
    private List<UserOperLogVO> pending = List.of();
    @Scheduled(fixedDelayString = "${peach.operation-log.flush-interval-millis:5000}")
    public void excuted() { handleUserOperLog(); }
    /** 串行处理待重试批次，避免手动触发和调度并行导致重复消费。 */
    public synchronized void handleUserOperLog() {
        if (pending.isEmpty()) pending = UserOperLogQueue.getInstance().drainBatch(200);
        if (pending.isEmpty()) return;
        try { userOperLogService.batchInsert(pending); pending = List.of(); }
        catch (RuntimeException ex) { log.error("Operation audit batch retained for retry, count={}, errorType={}", pending.size(), ex.getClass().getSimpleName()); }
    }
}
