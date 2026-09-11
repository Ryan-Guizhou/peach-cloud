package com.peach.redission.delayqueue.quickstart.example;

import com.peach.redission.delayqueue.context.DelayQueueContext;
import com.peach.redission.delayqueue.quickstart.consumer.OrderTimeoutConsumerTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 延迟队列样例：注入 {@link DelayQueueContext} 投递，由 {@link OrderTimeoutConsumerTask} 消费确认。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:05
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class OrderTimeoutDelayExample {

    private final DelayQueueContext delayQueueContext;
    private final OrderTimeoutConsumerTask consumerTask;

    /**
     * 短延迟投递单条订单超时消息并等待消费。
     */
    public String sendAndAwait(String orderId) {
        consumerTask.resetForExpect(1, orderId);
        delayQueueContext.sendMessage(
                OrderTimeoutConsumerTask.TOPIC,
                "orderId=" + orderId,
                1L,
                TimeUnit.SECONDS);
        boolean consumed;
        try {
            consumed = consumerTask.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("await interrupted", ex);
        }
        if (!consumed || consumerTask.getLastContent() == null
                || !consumerTask.getLastContent().contains(orderId)) {
            throw new IllegalStateException("consume failed for orderId=" + orderId);
        }
        log.info("sendAndAwait ok, content={}", consumerTask.getLastContent());
        return consumerTask.getLastContent();
    }

    /**
     * 多消息短延迟投递（可靠队列配置下）并等待全部消费。
     */
    public List<String> sendMultipleAndAwait(String orderPrefix, int count) {
        consumerTask.resetForExpect(count, orderPrefix);
        for (int i = 1; i <= count; i++) {
            delayQueueContext.sendMessage(
                    OrderTimeoutConsumerTask.TOPIC,
                    "orderId=" + orderPrefix + "-" + i,
                    1L,
                    TimeUnit.SECONDS);
        }
        boolean consumed;
        try {
            consumed = consumerTask.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("await interrupted", ex);
        }
        List<String> received = consumerTask.getReceived();
        if (!consumed || received.size() != count) {
            throw new IllegalStateException("multi consume failed, size=" + received.size());
        }
        log.info("sendMultipleAndAwait ok, size={}", received.size());
        return received;
    }
}
