package com.peach.rocket.example.service;

import org.springframework.stereotype.Indexed;
import com.peach.rocket.core.MqDelay;
import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.example.event.OrderCreatedEvent;
import com.peach.rocket.example.event.OrderPaidEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 示例订单消息发送服务。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
@Indexed
@Service
public class OrderService {

    @Resource
    private MqPublisher mqPublisher;

    /**
     * 发布一组订单消息示例。
     */
    public void publishDemoMessages() {
        OrderCreatedEvent createdEvent = new OrderCreatedEvent();
        createdEvent.setOrderId(10001L);
        createdEvent.setAmount(new BigDecimal("99.90"));
        createdEvent.setCreatedAt(LocalDateTime.now());
        mqPublisher.publish(createdEvent);
        mqPublisher.publishAsync(createdEvent);
        mqPublisher.publishDelay(createdEvent, MqDelay.duration(Duration.ofSeconds(10)));

        OrderPaidEvent paidEvent = new OrderPaidEvent();
        paidEvent.setOrderId(10001L);
        paidEvent.setPaidAt(LocalDateTime.now());
        mqPublisher.publishOrderly(paidEvent, String.valueOf(paidEvent.getOrderId()));
        log.info("[example-order-service] demo messages published. orderId={}", createdEvent.getOrderId());
    }
}
