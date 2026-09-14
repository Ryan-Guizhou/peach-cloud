package com.peach.rocket.quickstart.consumer;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.rocket.quickstart.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 订单创建消费者：记录已处理事件，供发布-消费闭环断言。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Component
@MqConsumer(topic = "order", tag = "created", consumerGroup = "peach-rocket-quickstart-order-created")
public class OrderCreatedConsumer implements MqMessageHandler<OrderCreatedEvent> {

    private final List<OrderCreatedEvent> handled = new CopyOnWriteArrayList<>();

    @Override
    public void handle(OrderCreatedEvent message, MqConsumeContext context) {
        handled.add(message);
        log.info("consumed order created, orderId={}, messageId={}", message.orderId(), context.messageId());
    }

    /**
     * @return 已消费事件快照
     */
    public List<OrderCreatedEvent> handled() {
        return List.copyOf(handled);
    }

    /**
     * 清空消费记录，便于演示与测试隔离。
     */
    public void reset() {
        handled.clear();
    }
}
