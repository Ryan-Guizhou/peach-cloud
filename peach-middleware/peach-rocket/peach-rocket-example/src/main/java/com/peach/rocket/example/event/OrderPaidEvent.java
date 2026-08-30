package com.peach.rocket.example.event;

import com.peach.rocket.annotation.MqEvent;
import java.time.LocalDateTime;

/**
 * OrderPaid事件。
 *
 * @param orderId 订单ID
 * @param paidAt 支付时间
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@MqEvent(topic = "order", tag = "paid", key = "#orderId")
public record OrderPaidEvent(Long orderId, LocalDateTime paidAt) {
}
