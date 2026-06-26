package com.peach.rocket.outbox;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Outbox 可靠消息补偿调度器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
public class MqOutboxDispatcher {

    private final MqOutboxStore outboxStore;
    private final RocketMQTemplate rocketMQTemplate;
    private final PeachRocketProperties properties;

    public MqOutboxDispatcher(MqOutboxStore outboxStore, RocketMQTemplate rocketMQTemplate, PeachRocketProperties properties) {
        this.outboxStore = outboxStore;
        this.rocketMQTemplate = rocketMQTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${peach.rocket.outbox.scan-interval-ms:2000}")
    public void dispatch() {
        List<MqOutboxEvent> events = outboxStore.findPending(properties.getOutbox().getBatchSize());
        for (MqOutboxEvent event : events) {
            try {
                String destination = event.getTag() == null || event.getTag().length() == 0 ? event.getTopic() : event.getTopic() + ":" + event.getTag();
                rocketMQTemplate.syncSend(destination, MessageBuilder.withPayload(event.getBody()).build());
                outboxStore.markSent(event.getMessageId());
                log.info("[mq-outbox] message sent. messageId={} topic={} tag={}", event.getMessageId(), event.getTopic(), event.getTag());
            } catch (RuntimeException ex) {
                outboxStore.markFailed(event.getMessageId());
                log.error("[mq-outbox-error] messageId={} topic={} tag={} exception={}", event.getMessageId(), event.getTopic(), event.getTag(), ex.getClass().getName(), ex);
            }
        }
    }
}
