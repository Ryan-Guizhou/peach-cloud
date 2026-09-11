package com.peach.redission.delayqueue.quickstart.scenario;

import com.peach.redission.delayqueue.core.ConsumerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 订单超时延迟队列消费者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
public class OrderTimeoutConsumerTask implements ConsumerTask {

    public static final String TOPIC = "order-timeout";

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutConsumerTask.class);

    private final AtomicReference<String> lastContent = new AtomicReference<>();
    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void execute(String content) {
        lastContent.set(content);
        latch.countDown();
        log.info("order-timeout delay message consumed, content={}", content);
    }

    @Override
    public String topic() {
        return TOPIC;
    }

    /**
     * @return 最近一次消费内容
     */
    public String getLastContent() {
        return lastContent.get();
    }

    /**
     * @return 消费等待闩锁
     */
    public CountDownLatch getLatch() {
        return latch;
    }
}
