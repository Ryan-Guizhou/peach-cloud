package com.peach.rocket.outbox;

import java.time.ZoneId;

import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.route.MqRoute;
import com.peach.rocket.route.MqRouteResolver;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DefaultMqOutboxPublisher相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
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
        MqMessageEnvelope<T> envelope = MqMessageEnvelope.create(
                UUID.randomUUID().toString(),
                route.topic(),
                route.tag(),
                route.key(),
                null,
                payload.getClass().getName(),
                1,
                headerResolver.resolve(actualOptions.getHeaders()),
                payload,
                LocalDateTime.now(ZoneId.systemDefault()));
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        MqOutboxEvent event = new MqOutboxEvent(
                envelope.messageId(),
                codec.encode(envelope),
                route.topic(),
                route.tag(),
                route.key(),
                actualOptions,
                MqOutboxStatus.INIT,
                0,
                now,
                now);
        outboxStore.save(event);
        return event.messageId();
    }
}
