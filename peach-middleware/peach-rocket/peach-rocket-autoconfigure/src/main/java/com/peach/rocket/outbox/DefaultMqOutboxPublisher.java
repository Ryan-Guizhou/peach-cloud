package com.peach.rocket.outbox;

import java.time.ZoneId;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.annotation.MqEvent;
import com.peach.rocket.producer.MqProducerMessageGuard;

import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.core.MqDelayLevels;
import com.peach.rocket.exception.MqException;
import org.springframework.util.StringUtils;
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

    private final PeachRocketProperties properties;
    private final MqOutboxStore outboxStore;
    private final MqMessageCodec codec;
    private final MqRouteResolver routeResolver;
    private final DefaultMqHeaderResolver headerResolver;

    public DefaultMqOutboxPublisher(MqOutboxStore outboxStore, MqMessageCodec codec, MqRouteResolver routeResolver, DefaultMqHeaderResolver headerResolver) {
        this(outboxStore, codec, routeResolver, headerResolver, new PeachRocketProperties());
    }

    public DefaultMqOutboxPublisher(MqOutboxStore outboxStore, MqMessageCodec codec,
            MqRouteResolver routeResolver, DefaultMqHeaderResolver headerResolver, PeachRocketProperties properties) {
        this.properties = properties;
        this.outboxStore = outboxStore;
        this.codec = codec;
        this.routeResolver = routeResolver;
        this.headerResolver = headerResolver;
    }

    @Override
    public <T> String publish(T payload, MqSendOptions options) {
        return publishEvent(payload, options, false);
    }

    @Override
    public <T> String publishStandalone(T payload, MqSendOptions options) {
        return publishEvent(payload, options, true);
    }

    private <T> String publishEvent(T payload, MqSendOptions options, boolean standalone) {
        if (payload == null) throw new MqException("Outbox payload is required");
        if ((properties.isProduction() || (!standalone && properties.getOutbox().isRequireTransaction())) && !outboxStore.isDurable()) {
            throw new MqException("Transactional Outbox requires a durable store");
        }
        MqSendOptions actualOptions = MqSendOptions.copyOf(options);
        if (properties.isProduction() && StringUtils.hasText(actualOptions.getShardingKey())) {
            throw new MqException("Production Outbox does not support strict orderly delivery");
        }
        if (actualOptions.getTimeoutMillis() < 0) throw new MqException("Outbox timeout must not be negative");
        Integer delayLevel = MqDelayLevels.resolve(actualOptions.getDelay());
        if (StringUtils.hasText(actualOptions.getShardingKey()) && delayLevel != null) {
            throw new MqException("Cannot combine RocketMQ shardingKey with delayed sending");
        }
        MqRoute route = routeResolver.resolve(payload, actualOptions);
        MqMessageEnvelope<T> envelope = MqMessageEnvelope.create(
                UUID.randomUUID().toString(),
                route.topic(),
                route.tag(),
                route.key(),
                properties.getAppName(),
                payload.getClass().getName(),
                payload.getClass().isAnnotationPresent(MqEvent.class) ? payload.getClass().getAnnotation(MqEvent.class).version() : 1,
                headerResolver.resolve(actualOptions.getHeaders()),
                payload,
                LocalDateTime.now(ZoneId.systemDefault()));
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        byte[] encoded = codec.encode(envelope);
        MqProducerMessageGuard.validate(encoded, envelope.headers(), route.key(), properties.getProducer());
        actualOptions.setHeaders(envelope.headers());
        MqOutboxEvent event = new MqOutboxEvent(
                envelope.messageId(),
                encoded,
                route.topic(),
                route.tag(),
                route.key(),
                actualOptions,
                MqOutboxStatus.INIT,
                0,
                now,
                now);
        if (standalone) outboxStore.saveStandalone(event); else outboxStore.save(event);
        return event.messageId();
    }
}
