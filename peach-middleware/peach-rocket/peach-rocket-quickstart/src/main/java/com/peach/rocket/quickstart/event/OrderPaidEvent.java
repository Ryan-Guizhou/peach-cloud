package com.peach.rocket.quickstart.event;

import com.peach.rocket.annotation.MqEvent;

import java.time.LocalDateTime;

/**
 * 订单支付事件，路由到 topic {@code order} / tag {@code paid}，适合按 orderId 顺序发送。
 *
 * @param orderId 订单 ID
 * @param paidAt  支付时间
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@MqEvent(topic = "order", tag = "paid", key = "#orderId")
public record OrderPaidEvent(Long orderId, LocalDateTime paidAt) {
}
