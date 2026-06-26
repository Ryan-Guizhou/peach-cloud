package com.peach.rocket.example.consumer;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.rocket.example.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单支付事件消费者。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
@Component
@MqConsumer(topic = "order", tag = "paid", consumerGroup = "peach-rocket-example-order-paid")
public class OrderPaidConsumer implements MqMessageHandler<OrderPaidEvent> {

    @Override
    public void handle(OrderPaidEvent message, MqConsumeContext context) {
        log.info("[example-order-paid] orderId={} paidAt={} messageId={}", message.getOrderId(), message.getPaidAt(), context.getMessageId());
    }
}
