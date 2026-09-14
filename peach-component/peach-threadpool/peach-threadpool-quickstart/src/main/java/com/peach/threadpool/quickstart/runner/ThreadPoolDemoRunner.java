package com.peach.threadpool.quickstart.runner;

import com.peach.threadpool.quickstart.example.AsyncExecutedExample;
import com.peach.threadpool.quickstart.example.ThreadPoolExecuteExample;
import com.peach.threadpool.quickstart.example.ThreadPoolSubmitExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示 ThreadPoolManager 提交/执行与 {@code @AsyncExecuted} 兼容用法。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:57
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.threadpool.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ThreadPoolDemoRunner implements ApplicationRunner {

    private static final String DEMO_REQUEST_ID = "qs-threadpool";

    private final ThreadPoolSubmitExample submitExample;
    private final ThreadPoolExecuteExample executeExample;
    private final AsyncExecutedExample asyncExecutedExample;

    @Override
    public void run(ApplicationArguments args) {
        runSubmitDemo();
        runExecuteDemo();
        runAsyncExecutedDemo();
        log.info("threadpool demo finished");
    }

    private void runSubmitDemo() {
        log.info("=== ThreadPoolManager submit Callable demo ===");
        ThreadPoolSubmitExample.SubmitObservation observed = submitExample.submitCallable(DEMO_REQUEST_ID);
        if (!DEMO_REQUEST_ID.equals(observed.getPayload()) || !isManagedIoThread(observed.getThreadName())) {
            throw new IllegalStateException("submit callable demo failed");
        }
        log.info("submit demo ok, payload={}, workerThread={}", observed.getPayload(), observed.getThreadName());
    }

    private void runExecuteDemo() {
        log.info("=== ThreadPoolManager execute / submit Runnable demo ===");
        String worker = executeExample.executeRunnable();
        if (!isManagedIoThread(worker)) {
            throw new IllegalStateException("execute runnable demo failed");
        }
        executeExample.submitRunnable();
        log.info("execute demo ok, workerThread={}", worker);
    }

    private void runAsyncExecutedDemo() {
        log.info("=== @AsyncExecuted demo ===");
        String worker = asyncExecutedExample.runOnIoPool();
        if (!isManagedIoThread(worker)) {
            throw new IllegalStateException("asyncExecuted demo failed");
        }
        log.info("asyncExecuted demo ok, workerThread={}", worker);
    }

    private static boolean isManagedIoThread(String threadName) {
        return threadName != null && threadName.startsWith("peach-pool-io-");
    }
}
