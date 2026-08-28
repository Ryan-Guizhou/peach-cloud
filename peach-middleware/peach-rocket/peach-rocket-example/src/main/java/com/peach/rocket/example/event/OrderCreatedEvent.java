package com.peach.rocket.example.event;

import com.peach.rocket.annotation.MqEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OrderCreated事件。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@MqEvent(topic = "order", tag = "created", key = "#orderId")
public record OrderCreatedEvent(Long orderId, BigDecimal amount, LocalDateTime createdAt) {
}
