package com.peach.rocket.example.consumer;

import org.springframework.stereotype.Indexed;
import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.rocket.example.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * OrderPaid消费者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Slf4j
@Indexed
@Component
@MqConsumer(topic = "order", tag = "paid", consumerGroup = "peach-rocket-example-order-paid")
public class OrderPaidConsumer implements MqMessageHandler<OrderPaidEvent> {

    @Override
    public void handle(OrderPaidEvent message, MqConsumeContext context) {
        log.info("[example-order-paid] orderId={} paidAt={} messageId={}", message.orderId(), message.paidAt(), context.messageId());
    }
}
