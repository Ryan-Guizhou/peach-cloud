package com.peach.rocket.quickstart.event;

import com.peach.rocket.annotation.MqEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建事件，路由到 topic {@code order} / tag {@code created}。
 *
 * @param orderId   订单 ID
 * @param amount    订单金额
 * @param createdAt 创建时间
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@MqEvent(topic = "order", tag = "created", key = "#orderId")
public record OrderCreatedEvent(Long orderId, BigDecimal amount, LocalDateTime createdAt) {
}
