package com.peach.rocket.example.consumer;

import org.springframework.stereotype.Indexed;
import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.rocket.example.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OrderCreated消费者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Slf4j
@Indexed
@Component
@MqConsumer(topic = "order", tag = "created", consumerGroup = "peach-rocket-example-order-created")
public class OrderCreatedConsumer implements MqMessageHandler<OrderCreatedEvent> {

    @Override
    public void handle(OrderCreatedEvent message, MqConsumeContext context) {
        log.info("[example-order-created] orderId={} amount={} messageId={}", message.orderId(), message.amount(), context.messageId());
    }
}
