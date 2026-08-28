package com.peach.rocket.outbox;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * MqOutboxDispatcher相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
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
                String destination = event.tag() == null || event.tag().isEmpty() ? event.topic() : event.topic() + ":" + event.tag();
                rocketMQTemplate.syncSend(destination, MessageBuilder.withPayload(event.body()).build());
                outboxStore.markSent(event.messageId());
                log.info("[mq-outbox] message sent. messageId={} topic={} tag={}", event.messageId(), event.topic(), event.tag());
            } catch (RuntimeException ex) {
                outboxStore.markFailed(event.messageId());
                log.error("[mq-outbox-error] messageId={} topic={} tag={} exception={}", event.messageId(), event.topic(), event.tag(), ex.getClass().getName(), ex);
            }
        }
    }
}
