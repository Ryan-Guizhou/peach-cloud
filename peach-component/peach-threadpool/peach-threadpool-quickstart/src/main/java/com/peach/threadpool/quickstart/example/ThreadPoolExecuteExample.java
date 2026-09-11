package com.peach.threadpool.quickstart.example;

import com.peach.threadpool.core.PoolType;
import com.peach.threadpool.manager.ThreadPoolManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 注入 {@link ThreadPoolManager#execute(PoolType, Runnable)} 与
 * {@link ThreadPoolManager#submit(PoolType, Runnable)}，演示无返回值提交。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:57
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ThreadPoolExecuteExample {

    private static final long AWAIT_SECONDS = 3L;

    private final ThreadPoolManager threadPoolManager;

    /**
     * 向 IO 池 {@code execute} 一个任务，等待其在受管线程上完成。
     *
     * @return 工作线程名
     */
    public String executeRunnable() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();
        threadPoolManager.execute(PoolType.IO, () -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });
        try {
            if (!latch.await(AWAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("execute runnable timed out");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("execute runnable interrupted", ex);
        }
        String worker = threadName.get();
        log.info("execute runnable finished, workerThread={}", worker);
        return worker;
    }

    /**
     * 向 IO 池 {@code submit} 一个 Runnable，并等待 {@link Future} 完成。
     */
    public void submitRunnable() {
        try {
            Runnable noop = () -> {
            };
            Future<?> future = threadPoolManager.submit(PoolType.IO, noop);
            future.get(AWAIT_SECONDS, TimeUnit.SECONDS);
            log.info("submit runnable finished");
        } catch (Exception ex) {
            throw new IllegalStateException("submit runnable failed", ex);
        }
    }
}
