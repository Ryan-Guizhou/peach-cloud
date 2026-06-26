package com.peach.rocket.outbox;

import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.route.MqRoute;
import com.peach.rocket.route.MqRouteResolver;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 默认 Outbox 可靠消息发布器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class DefaultMqOutboxPublisher implements MqOutboxPublisher {

    private final MqOutboxStore outboxStore;
    private final MqMessageCodec codec;
    private final MqRouteResolver routeResolver;
    private final DefaultMqHeaderResolver headerResolver;

    public DefaultMqOutboxPublisher(MqOutboxStore outboxStore, MqMessageCodec codec, MqRouteResolver routeResolver, DefaultMqHeaderResolver headerResolver) {
        this.outboxStore = outboxStore;
        this.codec = codec;
        this.routeResolver = routeResolver;
        this.headerResolver = headerResolver;
    }

    @Override
    public <T> String publish(T payload, MqSendOptions options) {
        MqSendOptions actualOptions = options == null ? MqSendOptions.defaults() : options;
        MqRoute route = routeResolver.resolve(payload, actualOptions);
        MqMessageEnvelope<T> envelope = new MqMessageEnvelope<T>();
        envelope.setMessageId(UUID.randomUUID().toString());
        envelope.setTopic(route.getTopic());
        envelope.setTag(route.getTag());
        envelope.setKey(route.getKey());
        envelope.setPayloadType(payload.getClass().getName());
        envelope.setPayload(payload);
        envelope.setHeaders(headerResolver.resolve(actualOptions.getHeaders()));
        envelope.setCreatedAt(LocalDateTime.now());
        MqOutboxEvent event = new MqOutboxEvent();
        event.setMessageId(envelope.getMessageId());
        event.setBody(codec.encode(envelope));
        event.setTopic(route.getTopic());
        event.setTag(route.getTag());
        event.setBusinessKey(route.getKey());
        event.setOptions(actualOptions);
        event.setStatus(MqOutboxStatus.INIT);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        outboxStore.save(event);
        return event.getMessageId();
    }
}
