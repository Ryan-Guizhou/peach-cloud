package com.peach.threadpool.quickstart.example;

import com.peach.threadpool.annoation.AsyncExecuted;
import com.peach.threadpool.core.PoolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 使用 {@link AsyncExecuted} 把方法体提交到指定池执行。
 *
 * <p>当前切面在 {@code async=true} 时仍会阻塞调用方等待 {@code Future.get()}，
 * 因此这是“池内执行 + 调用方等待”，不是 fire-and-forget。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:57
 */
@Slf4j
@Indexed
@Service
public class AsyncExecutedExample {

    /**
     * 在 IO 池线程上执行，返回该工作线程名。
     *
     * @return 切面提交后实际执行方法体的线程名
     */
    @AsyncExecuted(type = PoolType.IO)
    public String runOnIoPool() {
        String threadName = Thread.currentThread().getName();
        log.info("asyncExecuted method body, workerThread={}", threadName);
        return threadName;
    }
}
