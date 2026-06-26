package com.peach.rocket.example.event;

import com.peach.rocket.annotation.MqEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建事件。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@MqEvent(topic = "order", tag = "created", key = "#orderId")
public class OrderCreatedEvent {

    private Long orderId;

    private BigDecimal amount;

    private LocalDateTime createdAt;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
