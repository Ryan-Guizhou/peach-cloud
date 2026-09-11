package com.peach.virtualthread.quickstart.example;

import com.peach.virtualthread.annotation.VirtualGroup;
import com.peach.virtualthread.api.VirtualExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 按业务组注入 {@link VirtualExecutorService}，使用标准 {@code submit(Callable)} 等待结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Service
public class VirtualGroupSubmitExample {

    public static final String GROUP = "database";

    private static final long AWAIT_SECONDS = 3L;

    private final VirtualExecutorService databaseExecutor;

    public VirtualGroupSubmitExample(@VirtualGroup(GROUP) VirtualExecutorService databaseExecutor) {
        this.databaseExecutor = databaseExecutor;
    }

    /**
     * 把 payload 提交到 database 组，返回任务结果、虚拟线程名和组名。
     *
     * @param payload 演示用业务标识，不得包含敏感信息
     * @return 分组提交观察结果
     */
    public SubmitObservation submitCallable(String payload) {
        try {
            Future<SubmitObservation> future = databaseExecutor.submit(() ->
                    new SubmitObservation(payload, Thread.currentThread().getName(), databaseExecutor.groupName()));
            SubmitObservation observed = future.get(AWAIT_SECONDS, TimeUnit.SECONDS);
            log.info("grouped submit finished, payload={}, workerThread={}, group={}",
                    observed.getPayload(), observed.getThreadName(), observed.getGroupName());
            return observed;
        } catch (Exception ex) {
            throw new IllegalStateException("grouped submit failed", ex);
        }
    }

    /**
     * {@link VirtualExecutorService#submit(java.util.concurrent.Callable)} 的执行观察结果。
     */
    public static final class SubmitObservation {

        private final String payload;
        private final String threadName;
        private final String groupName;

        public SubmitObservation(String payload, String threadName, String groupName) {
            this.payload = payload;
            this.threadName = threadName;
            this.groupName = groupName;
        }

        public String getPayload() {
            return payload;
        }

        public String getThreadName() {
            return threadName;
        }

        public String getGroupName() {
            return groupName;
        }
    }
}
