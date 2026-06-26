package com.peach.rocket.example.event;

import com.peach.rocket.annotation.MqEvent;
import java.time.LocalDateTime;

/**
 * 订单支付事件。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@MqEvent(topic = "order", tag = "paid", key = "#orderId")
public class OrderPaidEvent {

    private Long orderId;

    private LocalDateTime paidAt;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
