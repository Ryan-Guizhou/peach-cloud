package com.peach.threadpool.quickstart.example;

import com.peach.threadpool.core.PoolType;
import com.peach.threadpool.manager.ThreadPoolManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 注入 {@link ThreadPoolManager#submit(PoolType, java.util.concurrent.Callable)}，
 * 提交有返回值任务并等待 {@link Future}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:57
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ThreadPoolSubmitExample {

    private static final long AWAIT_SECONDS = 3L;

    private final ThreadPoolManager threadPoolManager;

    /**
     * 把 payload 提交到 IO 池，返回工作线程回写的结果与线程名。
     *
     * @param payload 演示用业务标识，不得包含敏感信息
     * @return 任务结果与工作线程名
     */
    public SubmitObservation submitCallable(String payload) {
        Future<SubmitObservation> future = threadPoolManager.submit(PoolType.IO, () ->
                new SubmitObservation(payload, Thread.currentThread().getName()));
        try {
            SubmitObservation observed = future.get(AWAIT_SECONDS, TimeUnit.SECONDS);
            log.info("submit callable finished, payload={}, workerThread={}",
                    observed.getPayload(), observed.getThreadName());
            return observed;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new IllegalStateException("submit callable interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("submit callable failed", ex);
        }
    }

    /**
     * {@link ThreadPoolManager#submit(PoolType, java.util.concurrent.Callable)} 的执行观察结果。
     */
    public static final class SubmitObservation {

        private final String payload;
        private final String threadName;

        public SubmitObservation(String payload, String threadName) {
            this.payload = payload;
            this.threadName = threadName;
        }

        public String getPayload() {
            return payload;
        }

        public String getThreadName() {
            return threadName;
        }
    }
}
