package com.peach.rocket.producer;

import java.time.ZoneId;

import com.peach.rocket.annotation.MqEvent;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqDelay;
import com.peach.rocket.core.MqDelayLevels;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.metrics.MqMetrics;
import com.peach.rocket.route.MqRoute;
import com.peach.rocket.route.MqRouteResolver;
import com.peach.rocket.transaction.RocketMqTransactionMessageProducer;
import java.time.LocalDateTime;
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
 * RocketMqPublisher相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Slf4j
public class RocketMqPublisher implements MqPublisher {

    private final RocketMQTemplate rocketMQTemplate;
    private final MqMessageCodec codec;
    private final MqRouteResolver routeResolver;
    private final DefaultMqHeaderResolver headerResolver;
    private final PeachRocketProperties properties;
    private final RocketMqTransactionMessageProducer transactionMessageProducer;
    private final MqMetrics metrics;

    public RocketMqPublisher(RocketMQTemplate rocketMQTemplate, MqMessageCodec codec, MqRouteResolver routeResolver,
                             DefaultMqHeaderResolver headerResolver, PeachRocketProperties properties) {
        this(rocketMQTemplate, codec, routeResolver, headerResolver, properties, null);
    }

    public RocketMqPublisher(RocketMQTemplate rocketMQTemplate, MqMessageCodec codec, MqRouteResolver routeResolver,
                             DefaultMqHeaderResolver headerResolver, PeachRocketProperties properties,
                             RocketMqTransactionMessageProducer transactionMessageProducer) {
        this(rocketMQTemplate, codec, routeResolver, headerResolver, properties,
                transactionMessageProducer, MqMetrics.NOOP);
    }

    /**
     * 创建支持可选发送指标的 Producer。
     *
     * @param metrics 可选指标采集 SPI
     */
    public RocketMqPublisher(RocketMQTemplate rocketMQTemplate, MqMessageCodec codec, MqRouteResolver routeResolver,
                             DefaultMqHeaderResolver headerResolver, PeachRocketProperties properties,
                             RocketMqTransactionMessageProducer transactionMessageProducer, MqMetrics metrics) {
        this.metrics = metrics == null ? MqMetrics.NOOP : metrics;
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
        long metricStarted = System.nanoTime();
        try {
            MqSendResult result = publishSync(payload, options);
            metrics.publish("sync", result.isSuccess() ? "success" : "rejected",
                    System.nanoTime() - metricStarted);
            return result;
        } catch (RuntimeException ex) {
            metrics.publish("sync", "failed", System.nanoTime() - metricStarted);
            throw ex;
        }
    }

    private <T> MqSendResult publishSync(T payload, MqSendOptions options) {
        MqSendOptions actualOptions = prepareOptions(options);
        long start = System.currentTimeMillis();
        MqRoute route = routeResolver.resolve(payload, actualOptions);
        Integer delayLevel = MqDelayLevels.resolve(actualOptions.getDelay());
        String shardingKey = actualOptions.getShardingKey();
        if (StringUtils.hasText(shardingKey) && delayLevel != null) {
            throw new MqException("Cannot combine RocketMQ shardingKey with delayed sending");
        }
        Message<byte[]> message = buildMessage(payload, actualOptions, route);
        SendResult result;
        if (StringUtils.hasText(shardingKey)) {
            result = rocketMQTemplate.syncSendOrderly(destination(route), message,
                    shardingKey, actualOptions.getTimeoutMillis());
        } else if (delayLevel != null) {
            result = rocketMQTemplate.syncSend(destination(route), message,
                    actualOptions.getTimeoutMillis(), delayLevel);
        } else {
            result = rocketMQTemplate.syncSend(destination(route), message, actualOptions.getTimeoutMillis());
        }
        MqSendResult mqSendResult = toMqSendResult(result, route);
        log.info("[mq-send] app={} payloadType={} mode=sync topic={} tag={} key={} delayLevel={} messageId={} status={} cost={}ms success={}",
                properties.getAppName(), payload.getClass().getSimpleName(), mqSendResult.topic(), mqSendResult.tag(),
                mqSendResult.key(), delayLevel, mqSendResult.messageId(), mqSendResult.rawStatus(),
                System.currentTimeMillis() - start, mqSendResult.isSuccess());
        return mqSendResult;
    }

    @Override
    public <T> CompletableFuture<MqSendResult> publishAsync(final T payload) {
        return publishAsync(payload, MqSendOptions.defaults());
    }

    @Override
    public <T> CompletableFuture<MqSendResult> publishAsync(final T payload, MqSendOptions options) {
        final long metricStarted = System.nanoTime();
        final MqSendOptions actualOptions = prepareOptions(options);
        rejectUnsupportedAsyncOptions(actualOptions);
        final long start = System.currentTimeMillis();
        final MqRoute route = routeResolver.resolve(payload, actualOptions);
        final CompletableFuture<MqSendResult> future = new CompletableFuture<MqSendResult>();
        try {
            rocketMQTemplate.asyncSend(destination(route), buildMessage(payload, actualOptions, route),
                    new AsyncSendCallback(this, properties, payload, route, start, metricStarted, future),
                    actualOptions.getTimeoutMillis());
        } catch (RuntimeException ex) {
            metrics.publish("async", "failed", System.nanoTime() - metricStarted);
            throw ex;
        }
        return future;
    }

    @Override
    public <T> void publishOneWay(T payload, MqSendOptions options) {
        long started = System.nanoTime();
        try {
            sendOneWay(payload, options);
            metrics.publish("oneway", "submitted", System.nanoTime() - started);
        } catch (RuntimeException ex) {
            metrics.publish("oneway", "failed", System.nanoTime() - started);
            throw ex;
        }
    }

    private <T> void sendOneWay(T payload, MqSendOptions options) {
        MqSendOptions actualOptions = prepareOptions(options);
        rejectUnsupportedAsyncOptions(actualOptions);
        MqRoute route = routeResolver.resolve(payload, actualOptions);
        rocketMQTemplate.sendOneWay(destination(route), buildMessage(payload, actualOptions, route));
        log.info("[mq-send] app={} payloadType={} mode=one-way topic={} tag={} key={} submitted=true",
                properties.getAppName(), payload.getClass().getSimpleName(), route.topic(), route.tag(), route.key());
    }

    @Override
    public <T> MqSendResult publishOrderly(T payload, String shardingKey) {
        if (!StringUtils.hasText(shardingKey)) {
            throw new MqException("shardingKey must not be blank for orderly message");
        }
        return publish(payload, MqSendOptions.builder().shardingKey(shardingKey).build());
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

    /**
     * 防止异步/单向发送静默丢弃顺序或延迟选项；调用方可改用同步发送。
     */
    private void rejectUnsupportedAsyncOptions(MqSendOptions options) {
        if (StringUtils.hasText(options.getShardingKey()) || options.getDelay() != null) {
            throw new MqException("Async/one-way MQ publishing does not support shardingKey or delay options");
        }
    }

    private MqSendOptions prepareOptions(MqSendOptions options) {
        MqSendOptions actualOptions = MqSendOptions.copyOf(options);
        if (actualOptions.getTimeoutMillis() <= 0) {
            actualOptions.setTimeoutMillis(properties.getProducer().getDefaultTimeout().toMillis());
        }
        return actualOptions;
    }

    private <T> Message<byte[]> buildMessage(T payload, MqSendOptions options, MqRoute route) {
        MqMessageEnvelope<T> envelope = MqMessageEnvelope.create(
                UUID.randomUUID().toString(),
                route.topic(),
                route.tag(),
                route.key(),
                properties.getAppName(),
                payload.getClass().getName(),
                resolveVersion(payload),
                headerResolver.resolve(options.getHeaders()),
                payload,
                LocalDateTime.now(ZoneId.systemDefault()));
        MessageBuilder<byte[]> builder = MessageBuilder.withPayload(codec.encode(envelope));
        if (StringUtils.hasText(route.key())) {
            builder.setHeader(MessageConst.PROPERTY_KEYS, route.key());
        }
        for (String name : envelope.headers().keySet()) {
            builder.setHeader(name, envelope.headers().get(name));
        }
        return builder.build();
    }

    private String destination(MqRoute route) {
        return StringUtils.hasText(route.tag()) ? route.topic() + ":" + route.tag() : route.topic();
    }

    private MqSendResult toMqSendResult(SendResult result, MqRoute route) {
        return MqSendResult.builder()
                .success(result != null && result.getSendStatus() == SendStatus.SEND_OK)
                .messageId(result == null ? null : result.getMsgId())
                .topic(route.topic()).tag(route.tag()).key(route.key())
                .rawStatus(result == null || result.getSendStatus() == null ? null : result.getSendStatus().name())
                .build();
    }

    private int resolveVersion(Object payload) {
        MqEvent event = payload.getClass().getAnnotation(MqEvent.class);
        return event == null ? 1 : event.version();
    }

    /**
     * AsyncSend回调。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private static final class AsyncSendCallback implements SendCallback {

        private final RocketMqPublisher publisher;
        private final PeachRocketProperties properties;
        private final Object payload;
        private final MqRoute route;
        private final long start;
        private final long metricStarted;
        private final CompletableFuture<MqSendResult> future;

        private AsyncSendCallback(RocketMqPublisher publisher,
                                  PeachRocketProperties properties,
                                  Object payload,
                                  MqRoute route,
                                  long start, long metricStarted,
                                  CompletableFuture<MqSendResult> future) {
            this.publisher = publisher;
            this.properties = properties;
            this.payload = payload;
            this.route = route;
            this.start = start;
            this.metricStarted = metricStarted;
            this.future = future;
        }

        @Override
        public void onSuccess(SendResult sendResult) {
            MqSendResult result = publisher.toMqSendResult(sendResult, route);
            log.info("[mq-send] app={} payloadType={} mode=async topic={} tag={} key={} messageId={} status={} cost={}ms success={}",
                    properties.getAppName(), payload.getClass().getSimpleName(), result.topic(), result.tag(),
                    result.key(), result.messageId(), result.rawStatus(), System.currentTimeMillis() - start, result.isSuccess());
            publisher.metrics.publish("async", result.isSuccess() ? "success" : "rejected",
                    System.nanoTime() - metricStarted);
            future.complete(result);
        }

        @Override
        public void onException(Throwable throwable) {
            log.error("[mq-send-error] app={} payloadType={} mode=async topic={} tag={} key={} cost={}ms exception={}",
                    properties.getAppName(), payload.getClass().getSimpleName(), route.topic(), route.tag(), route.key(),
                    System.currentTimeMillis() - start, throwable.getClass().getName(), throwable);
            publisher.metrics.publish("async", "failed", System.nanoTime() - metricStarted);
            future.completeExceptionally(throwable);
        }
    }

}
