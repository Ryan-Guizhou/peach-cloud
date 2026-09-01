package com.peach.rocket.example.service;

import java.time.ZoneId;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import com.peach.rocket.core.MqDelay;
import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.example.event.OrderCreatedEvent;
import com.peach.rocket.example.event.OrderPaidEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Order服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MqPublisher mqPublisher;

    /**
     * 发布一组订单消息示例。
     */
    public void publishDemoMessages() {
        OrderCreatedEvent createdEvent = new OrderCreatedEvent(
                10001L, new BigDecimal("99.90"), LocalDateTime.now(ZoneId.systemDefault()));
        mqPublisher.publish(createdEvent);
        mqPublisher.publishAsync(createdEvent);
        mqPublisher.publishDelay(createdEvent, MqDelay.duration(Duration.ofSeconds(10)));

        OrderPaidEvent paidEvent = new OrderPaidEvent(10001L, LocalDateTime.now(ZoneId.systemDefault()));
        mqPublisher.publishOrderly(paidEvent, String.valueOf(paidEvent.orderId()));
        log.info("[example-order-service] demo messages published. orderId={}", createdEvent.orderId());
    }
}
