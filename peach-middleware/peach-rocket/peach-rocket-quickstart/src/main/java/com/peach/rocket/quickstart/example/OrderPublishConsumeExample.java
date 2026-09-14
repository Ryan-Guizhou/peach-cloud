package com.peach.rocket.quickstart.example;

import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.quickstart.consumer.OrderCreatedConsumer;
import com.peach.rocket.quickstart.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 订单创建发布-消费闭环：{@link MqPublisher#publish} 后同步投递给 {@code @MqConsumer}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class OrderPublishConsumeExample {

    private final MqPublisher mqPublisher;
    private final OrderCreatedConsumer createdConsumer;

    /**
     * 发布一条订单创建事件，并由匹配的消费者处理。
     *
     * @param orderId 订单 ID
     * @return Broker 无关的发送结果
     */
    public MqSendResult publishCreated(long orderId) {
        createdConsumer.reset();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, new BigDecimal("99.90"), LocalDateTime.now(ZoneId.systemDefault()));
        MqSendResult result = mqPublisher.publish(event);
        log.info("published order created, orderId={}, success={}, consumed={}",
                orderId, result.isSuccess(), createdConsumer.handled().size());
        return result;
    }

    /**
     * @return 本次发布后已消费的创建事件
     */
    public List<OrderCreatedEvent> consumedCreated() {
        return createdConsumer.handled();
    }
}
