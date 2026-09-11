package com.peach.threadpool.quickstart.scenario;

import com.peach.threadpool.core.PoolType;
import com.peach.threadpool.manager.ThreadPoolManager;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 存量 ThreadPoolManager 兼容调用场景（非新业务模板）。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Service
public class ThreadPoolCompatScenarioService {

    private final ThreadPoolManager threadPoolManager;

    /**
     * @param threadPoolManager 存量线程池管理器
     */
    public ThreadPoolCompatScenarioService(ThreadPoolManager threadPoolManager) {
        this.threadPoolManager = threadPoolManager;
    }

    /**
     * 通过旧 API 提交一次 IO 池任务并等待结果。
     *
     * @return 任务结果
     */
    public String runOnce() throws ExecutionException, InterruptedException, TimeoutException {
        Future<String> future = threadPoolManager.submit(PoolType.IO, () -> "compat-ok");
        return future.get(3, TimeUnit.SECONDS);
    }
}
