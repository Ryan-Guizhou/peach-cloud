package com.peach.rocket.outbox;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.exception.MqException;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Outbox 消息投递器。Broker 发送与数据库确认分别处理，ACK 失败时等待租约恢复。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 */
@Slf4j
public class MqOutboxDispatcher {

    private final MqOutboxStore outboxStore;
    private final RocketMQTemplate rocketMQTemplate;
    private final PeachRocketProperties properties;

    public MqOutboxDispatcher(MqOutboxStore outboxStore, RocketMQTemplate rocketMQTemplate,
                              PeachRocketProperties properties) {
        this.outboxStore = outboxStore;
        this.rocketMQTemplate = rocketMQTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${peach.rocket.outbox.scan-interval-ms:2000}")
    public void dispatch() {
        for (MqOutboxClaim claim : outboxStore.claimPending(properties.getOutbox().getBatchSize())) {
            MqOutboxEvent event = claim.event();
            try {
                String destination = event.tag() == null || event.tag().isEmpty()
                        ? event.topic() : event.topic() + ":" + event.tag();
                MqSendOptions options = event.options();
                long timeout = options != null && options.getTimeoutMillis() > 0
                        ? options.getTimeoutMillis()
                        : properties.getProducer().getDefaultTimeout().toMillis();
                SendResult result = rocketMQTemplate.syncSend(
                        destination, MessageBuilder.withPayload(event.body()).build(), timeout);
                if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
                    throw new MqException("Broker did not confirm Outbox message, status="
                            + (result == null ? "null" : result.getSendStatus()));
                }
            } catch (RuntimeException sendError) {
                recordSendFailure(claim, sendError);
                continue;
            }

            try {
                outboxStore.markSent(claim);
                log.debug("[mq-outbox] message sent. messageId={} topic={} tag={}",
                        event.messageId(), event.topic(), event.tag());
            } catch (RuntimeException ackError) {
                // Broker 已确认，但 DB ACK 不确定。不得反向标记失败，只能等待租约回收。
                log.error("[mq-outbox-ack-error] Broker confirmed, store ACK failed; "
                                + "lease recovery required. messageId={} topic={} exception={}",
                        event.messageId(), event.topic(), ackError.getClass().getName(), ackError);
            }
        }
    }

    private void recordSendFailure(MqOutboxClaim claim, RuntimeException sendError) {
        MqOutboxEvent event = claim.event();
        try {
            outboxStore.markFailed(claim);
        } catch (RuntimeException storeError) {
            log.error("[mq-outbox-store-error] Failed to record send failure; "
                            + "lease recovery required. messageId={} topic={} exception={}",
                    event.messageId(), event.topic(), storeError.getClass().getName(), storeError);
        }
        log.error("[mq-outbox-error] messageId={} topic={} tag={} exception={}",
                event.messageId(), event.topic(), event.tag(),
                sendError.getClass().getName(), sendError);
    }
}
