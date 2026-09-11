package com.peach.rocket.quickstart.example;

import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.quickstart.config.InProcessMqPublisher;
import com.peach.rocket.quickstart.consumer.OrderPaidConsumer;
import com.peach.rocket.quickstart.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 订单支付顺序发布：使用稳定 {@code orderId} 作为 shardingKey，再投递给 paid 消费者。
 *
 * <p>进程内发布器验证 shardingKey 契约与消费闭环；Broker 队列级顺序需要真实 RocketMQ。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class OrderOrderlyPublishExample {

    private final MqPublisher mqPublisher;
    private final InProcessMqPublisher inProcessMqPublisher;
    private final OrderPaidConsumer paidConsumer;

    /**
     * 按订单 ID 顺序发布支付事件。
     *
     * @param orderId 订单 ID，同时作为 shardingKey
     * @return 发送结果
     */
    public MqSendResult publishPaidOrderly(long orderId) {
        paidConsumer.reset();
        inProcessMqPublisher.reset();
        OrderPaidEvent event = new OrderPaidEvent(orderId, LocalDateTime.now(ZoneId.systemDefault()));
        String shardingKey = String.valueOf(orderId);
        MqSendResult result = mqPublisher.publishOrderly(event, shardingKey);
        log.info("published order paid orderly, orderId={}, shardingKey={}, consumed={}",
                orderId, shardingKey, paidConsumer.handled().size());
        return result;
    }

    /**
     * @return 已记录的顺序分片键
     */
    public List<String> orderlyShardingKeys() {
        return inProcessMqPublisher.orderlyShardingKeys();
    }

    /**
     * @return 本次发布后已消费的支付事件
     */
    public List<OrderPaidEvent> consumedPaid() {
        return paidConsumer.handled();
    }
}
