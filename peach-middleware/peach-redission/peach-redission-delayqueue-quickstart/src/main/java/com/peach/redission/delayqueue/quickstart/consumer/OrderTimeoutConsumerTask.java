package com.peach.redission.delayqueue.quickstart.consumer;

import com.peach.redission.delayqueue.core.ConsumerTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 订单超时延迟队列消费者：实现 {@link ConsumerTask}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:05
 */
@Slf4j
@Indexed
@Component
public class OrderTimeoutConsumerTask implements ConsumerTask {

    public static final String TOPIC = "order-timeout";

    private final AtomicReference<CountDownLatch> latchRef = new AtomicReference<>(new CountDownLatch(0));
    private final AtomicReference<String> expectContains = new AtomicReference<>();
    private final AtomicReference<String> lastContent = new AtomicReference<>();
    private final List<String> received = new CopyOnWriteArrayList<>();

    @Override
    public void execute(String content) {
        String filter = expectContains.get();
        if (filter != null && (content == null || !content.contains(filter))) {
            log.info("order-timeout ignored by filter, content={}", content);
            return;
        }
        lastContent.set(content);
        received.add(content);
        CountDownLatch latch = latchRef.get();
        if (latch != null) {
            latch.countDown();
        }
        log.info("order-timeout consumed, content={}", content);
    }

    @Override
    public String topic() {
        return TOPIC;
    }

    public void resetForExpect(int expected, String contentContains) {
        lastContent.set(null);
        received.clear();
        expectContains.set(contentContains);
        latchRef.set(new CountDownLatch(Math.max(expected, 0)));
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        CountDownLatch latch = latchRef.get();
        return latch != null && latch.await(timeout, unit);
    }

    public String getLastContent() {
        return lastContent.get();
    }

    public List<String> getReceived() {
        return new ArrayList<>(received);
    }
}
