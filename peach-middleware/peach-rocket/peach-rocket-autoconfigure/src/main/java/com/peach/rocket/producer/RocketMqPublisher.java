package com.peach.rocket.producer;

import java.time.ZoneId;

import com.peach.rocket.annotation.MqEvent;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqDelay;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.route.MqRoute;
import com.peach.rocket.route.MqRouteResolver;
import com.peach.rocket.transaction.RocketMqTransactionMessageProducer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

/**
 * 基于 {@link RocketMQTemplate} 的 MQ 生产者适配器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
public class RocketMqPublisher implements MqPublisher {

    private static final List<DelayLevel> DEFAULT_DELAY_LEVELS = Collections.unmodifiableList(Arrays.asList(
            new DelayLevel(1, Duration.ofSeconds(1)), new DelayLevel(2, Duration.ofSeconds(5)),
            new DelayLevel(3, Duration.ofSeconds(10)), new DelayLevel(4, Duration.ofSeconds(30)),
            new DelayLevel(5, Duration.ofMinutes(1)), new DelayLevel(6, Duration.ofMinutes(2)),
            new DelayLevel(7, Duration.ofMinutes(3)), new DelayLevel(8, Duration.ofMinutes(4)),
            new DelayLevel(9, Duration.ofMinutes(5)), new DelayLevel(10, Duration.ofMinutes(6)),
            new DelayLevel(11, Duration.ofMinutes(7)), new DelayLevel(12, Duration.ofMinutes(8)),
            new DelayLevel(13, Duration.ofMinutes(9)), new DelayLevel(14, Duration.ofMinutes(10)),
            new DelayLevel(15, Duration.ofMinutes(20)), new DelayLevel(16, Duration.ofMinutes(30)),
            new DelayLevel(17, Duration.ofHours(1)), new DelayLevel(18, Duration.ofHours(2))));

    private final RocketMQTemplate rocketMQTemplate;
    private final MqMessageCodec codec;
    private final MqRouteResolver routeResolver;
    private final DefaultMqHeaderResolver headerResolver;
    private final PeachRocketProperties properties;
    private final RocketMqTransactionMessageProducer transactionMessageProducer;

    public RocketMqPublisher(RocketMQTemplate rocketMQTemplate, MqMessageCodec codec, MqRouteResolver routeResolver,
                             DefaultMqHeaderResolver headerResolver, PeachRocketProperties properties) {
        this(rocketMQTemplate, codec, routeResolver, headerResolver, properties, null);
    }

    public RocketMqPublisher(RocketMQTemplate rocketMQTemplate, MqMessageCodec codec, MqRouteResolver routeResolver,
                             DefaultMqHeaderResolver headerResolver, PeachRocketProperties properties,
                             RocketMqTransactionMessageProducer transactionMessageProducer) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.codec = codec;
        this.routeResolver = routeResolver;
        this.headerResolver = headerResolver;
        this.properties = properties;
        this.transactionMessageProducer = transactionMessageProducer;
    }

    @Override
    public <T> MqSendResult publish(T payload) {
        return publish(payload, MqSendOptions.defaults());
    }

    @Override
    public <T> MqSendResult publish(T payload, MqSendOptions options) {
        MqSendOptions actualOptions = prepareOptions(options);
        long start = System.currentTimeMillis();
        MqRoute route = routeResolver.resolve(payload, actualOptions);
        Integer delayLevel = resolveDelayLevel(actualOptions.getDelay());
        SendResult result = delayLevel == null
                ? rocketMQTemplate.syncSend(destination(route), buildMessage(payload, actualOptions, route), actualOptions.getTimeoutMillis())
                : rocketMQTemplate.syncSend(destination(route), buildMessage(payload, actualOptions, route), actualOptions.getTimeoutMillis(), delayLevel);
        MqSendResult mqSendResult = toMqSendResult(result, route);
        log.info("[mq-send] app={} payloadType={} mode=sync topic={} tag={} key={} delayLevel={} messageId={} status={} cost={}ms success={}",
                properties.getAppName(), payload.getClass().getSimpleName(), mqSendResult.getTopic(), mqSendResult.getTag(),
                mqSendResult.getKey(), delayLevel, mqSendResult.getMessageId(), mqSendResult.getRawStatus(),
                System.currentTimeMillis() - start, mqSendResult.isSuccess());
        return mqSendResult;
    }

    @Override
    public <T> CompletableFuture<MqSendResult> publishAsync(final T payload) {
        return publishAsync(payload, MqSendOptions.defaults());
    }

    @Override
    public <T> CompletableFuture<MqSendResult> publishAsync(final T payload, MqSendOptions options) {
        final MqSendOptions actualOptions = prepareOptions(options);
        final long start = System.currentTimeMillis();
        final MqRoute route = routeResolver.resolve(payload, actualOptions);
        final CompletableFuture<MqSendResult> future = new CompletableFuture<MqSendResult>();
        rocketMQTemplate.asyncSend(destination(route), buildMessage(payload, actualOptions, route), new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                MqSendResult result = toMqSendResult(sendResult, route);
                log.info("[mq-send] app={} payloadType={} mode=async topic={} tag={} key={} messageId={} status={} cost={}ms success={}",
                        properties.getAppName(), payload.getClass().getSimpleName(), result.getTopic(), result.getTag(),
                        result.getKey(), result.getMessageId(), result.getRawStatus(), System.currentTimeMillis() - start, result.isSuccess());
                future.complete(result);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("[mq-send-error] app={} payloadType={} mode=async topic={} tag={} key={} cost={}ms exception={}",
                        properties.getAppName(), payload.getClass().getSimpleName(), route.getTopic(), route.getTag(), route.getKey(),
                        System.currentTimeMillis() - start, throwable.getClass().getName(), throwable);
                future.completeExceptionally(throwable);
            }
        }, actualOptions.getTimeoutMillis());
        return future;
    }

    @Override
    public <T> void publishOneWay(T payload, MqSendOptions options) {
        MqSendOptions actualOptions = prepareOptions(options);
        MqRoute route = routeResolver.resolve(payload, actualOptions);
        rocketMQTemplate.sendOneWay(destination(route), buildMessage(payload, actualOptions, route));
        log.info("[mq-send] app={} payloadType={} mode=one-way topic={} tag={} key={} success=true",
                properties.getAppName(), payload.getClass().getSimpleName(), route.getTopic(), route.getTag(), route.getKey());
    }

    @Override
    public <T> MqSendResult publishOrderly(T payload, String shardingKey) {
        if (!StringUtils.hasText(shardingKey)) {
            throw new MqException("shardingKey must not be blank for orderly message");
        }
        MqSendOptions options = MqSendOptions.builder().shardingKey(shardingKey).build();
        MqRoute route = routeResolver.resolve(payload, options);
        SendResult result = rocketMQTemplate.syncSendOrderly(destination(route), buildMessage(payload, options, route), shardingKey);
        return toMqSendResult(result, route);
    }

    @Override
    public <T> MqSendResult publishDelay(T payload, MqDelay delay) {
        if (delay == null) {
            throw new MqException("delay must not be null");
        }
        return publish(payload, MqSendOptions.builder().delay(delay).build());
    }

    @Override
    public <T> MqSendResult publishTransaction(T payload, String transactionKey) {
        return publishTransaction(payload, transactionKey, MqSendOptions.defaults());
    }

    @Override
    public <T> MqSendResult publishTransaction(T payload, String transactionKey, MqSendOptions options) {
        if (transactionMessageProducer == null) {
            throw new MqException("RocketMQ transaction message is disabled or no transaction producer bean is available");
        }
        return transactionMessageProducer.sendTransaction(payload, transactionKey, options);
    }

    private MqSendOptions prepareOptions(MqSendOptions options) {
        MqSendOptions actualOptions = options == null ? MqSendOptions.defaults() : options;
        if (actualOptions.getTimeoutMillis() <= 0) {
            actualOptions.setTimeoutMillis(properties.getProducer().getDefaultTimeout().toMillis());
        }
        return actualOptions;
    }

    private <T> Message<byte[]> buildMessage(T payload, MqSendOptions options, MqRoute route) {
        MqMessageEnvelope<T> envelope = new MqMessageEnvelope<T>();
        envelope.setMessageId(UUID.randomUUID().toString());
        envelope.setTopic(route.getTopic());
        envelope.setTag(route.getTag());
        envelope.setKey(route.getKey());
        envelope.setProducerApp(properties.getAppName());
        envelope.setVersion(resolveVersion(payload));
        envelope.setPayloadType(payload.getClass().getName());
        envelope.setPayload(payload);
        envelope.setHeaders(headerResolver.resolve(options.getHeaders()));
        envelope.setCreatedAt(LocalDateTime.now(ZoneId.systemDefault()));
        MessageBuilder<byte[]> builder = MessageBuilder.withPayload(codec.encode(envelope));
        if (StringUtils.hasText(route.getKey())) {
            builder.setHeader(MessageConst.PROPERTY_KEYS, route.getKey());
        }
        for (String name : envelope.getHeaders().keySet()) {
            builder.setHeader(name, envelope.getHeaders().get(name));
        }
        return builder.build();
    }

    private String destination(MqRoute route) {
        return StringUtils.hasText(route.getTag()) ? route.getTopic() + ":" + route.getTag() : route.getTopic();
    }

    private MqSendResult toMqSendResult(SendResult result, MqRoute route) {
        return MqSendResult.builder()
                .success(result != null && result.getSendStatus() == SendStatus.SEND_OK)
                .messageId(result == null ? null : result.getMsgId())
                .topic(route.getTopic()).tag(route.getTag()).key(route.getKey())
                .rawStatus(result == null || result.getSendStatus() == null ? null : result.getSendStatus().name())
                .build();
    }

    private int resolveVersion(Object payload) {
        MqEvent event = payload.getClass().getAnnotation(MqEvent.class);
        return event == null ? 1 : event.version();
    }

    private Integer resolveDelayLevel(MqDelay delay) {
        if (delay == null) {
            return null;
        }
        if (delay.getRocketMqDelayLevel() != null) {
            return delay.getRocketMqDelayLevel();
        }
        Duration duration = delay.getDuration();
        for (DelayLevel level : DEFAULT_DELAY_LEVELS) {
            if (!level.getDuration().minus(duration).isNegative()) {
                return level.getLevel();
            }
        }
        return DEFAULT_DELAY_LEVELS.get(DEFAULT_DELAY_LEVELS.size() - 1).getLevel();
    }

    private static class DelayLevel {
        private final int level;
        private final Duration duration;
        DelayLevel(int level, Duration duration) { this.level = level; this.duration = duration; }
        int getLevel() { return level; }
        Duration getDuration() { return duration; }
    }
}
