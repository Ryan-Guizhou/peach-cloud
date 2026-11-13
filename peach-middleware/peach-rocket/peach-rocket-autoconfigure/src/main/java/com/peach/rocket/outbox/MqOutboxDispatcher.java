package com.peach.rocket.outbox;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.producer.MqProducerMessageGuard;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.core.MqDelayLevels;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.metrics.MqMetrics;
import com.peach.rocket.metrics.MqMetricsGuard;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Outbox 消息投递器。Broker 发送与数据库确认分别处理，ACK 失败时等待租约恢复。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12
 */
@Slf4j
public class MqOutboxDispatcher {

    private final MqOutboxStore outboxStore;
    private final RocketMQTemplate rocketMQTemplate;
    private final PeachRocketProperties properties;
    private final ExecutorService parallelExecutor;
    private final MqMetrics metrics;

    public MqOutboxDispatcher(MqOutboxStore outboxStore, RocketMQTemplate rocketMQTemplate,
                              PeachRocketProperties properties) {
        this(outboxStore, rocketMQTemplate, properties, null);
    }

    /**
     * 可选复用已经由 peach-virtual-thread 管理的有界业务组执行器。
     *
     * @param outboxStore 出站消息存储
     * @param rocketMQTemplate RocketMQ 客户端
     * @param properties 发送配置
     * @param parallelExecutor 可选外部受管执行器；禁止在此创建新线程池
     */
    public MqOutboxDispatcher(MqOutboxStore outboxStore, RocketMQTemplate rocketMQTemplate,
                              PeachRocketProperties properties, ExecutorService parallelExecutor) {
        this(outboxStore, rocketMQTemplate, properties, parallelExecutor, MqMetrics.NOOP);
    }

    /**
     * 创建支持可选发送指标的 Outbox 投递器。
     *
     * @param metrics 可选 MQ 指标 SPI
     */
    public MqOutboxDispatcher(MqOutboxStore outboxStore, RocketMQTemplate rocketMQTemplate,
                              PeachRocketProperties properties, ExecutorService parallelExecutor,
                              MqMetrics metrics) {
        this.metrics = MqMetricsGuard.of(metrics);
        if (properties.getOutbox().isParallelEnabled() && parallelExecutor == null) {
            throw new IllegalStateException("Outbox parallel mode requires a managed virtual-thread executor");
        }
        this.outboxStore = outboxStore;
        this.rocketMQTemplate = rocketMQTemplate;
        this.properties = properties;
        this.parallelExecutor = parallelExecutor;
    }

    /**
     * 每次只认领一条消息，完成投递与确认后才认领下一条；每轮不超过配置的批量上限。
     * 普通消息可交给受管执行器，但仍逐条等待完成，避免排队消息在首次尝试前耗尽租约。
     */
    @Scheduled(fixedDelayString = "${peach.rocket.outbox.scan-interval-ms:2000}")
    public void dispatch() {
        for (int attempted = 0; attempted < properties.getOutbox().getBatchSize(); attempted++) {
            List<MqOutboxClaim> claims = outboxStore.claimPending(1);
            if (CollectionUtils.isEmpty(claims)) {
                return;
            }
            MqOutboxClaim claim = claims.getFirst();
            if (!properties.getOutbox().isParallelEnabled() || parallelExecutor == null
                    || requiresSerialDelivery(claims)) {
                deliver(claim);
                continue;
            }
            Future<?> task;
            try {
                task = parallelExecutor.submit(() -> deliver(claim));
            } catch (RejectedExecutionException rejected) {
                recordSendFailure(claim, rejected);
                continue;
            }
            try {
                task.get();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                log.warn("[mq-outbox] dispatch interrupted; unfinished claims will recover by lease");
                return;
            } catch (ExecutionException failure) {
                Throwable cause = failure.getCause() == null ? failure : failure.getCause();
                log.error("[mq-outbox] worker failed unexpectedly; claim lease recovery may be required. exception={}",
                        cause.getClass().getName(), cause);
            }
        }
    }

    private boolean requiresSerialDelivery(List<MqOutboxClaim> claims) {
        return claims.stream().anyMatch(claim -> {
            MqSendOptions options = claim.event().options();
            return options != null && (StringUtils.hasText(options.getShardingKey())
                    || options.getDelay() != null);
        });
    }

    /**
     * 对单条已认领消息执行发送与 token-scoped ACK。
     * Broker 确认后数据库 ACK 失败只能等待租约重试，不可错误标记发送失败。
     *
     * @param claim 带认领令牌的消息快照
     */
    private void deliver(MqOutboxClaim claim) {
        MqOutboxEvent event = claim.event();
        try {
            String destination = StringUtils.hasText(event.tag())
                    ? event.topic() + ":" + event.tag() : event.topic();
            MqSendOptions options = event.options();
            long timeout = options != null && options.getTimeoutMillis() > 0
                    ? options.getTimeoutMillis()
                    : properties.getProducer().getDefaultTimeout().toMillis();
            MqProducerMessageGuard.validate(event.body(), options == null ? null : options.getHeaders(),
                    event.businessKey(), properties.getProducer());
            if (timeout <= 0 || (outboxStore.isDurable()
                    && timeout >= properties.getJdbc().getOutboxLease().toMillis())) {
                throw new MqException("Outbox send timeout must be positive and shorter than its lease");
            }
            String shardingKey = options == null ? null : options.getShardingKey();
            if (properties.isProduction() && StringUtils.hasText(shardingKey)) {
                throw new MqException("Production Outbox does not support strict orderly delivery");
            }
            Integer delayLevel = options == null ? null : MqDelayLevels.resolve(options.getDelay());
            if (StringUtils.hasText(shardingKey) && delayLevel != null) {
                throw new MqException("Outbox cannot combine orderly and delayed sending");
            }

            MessageBuilder<byte[]> messageBuilder = MessageBuilder.withPayload(event.body());
            if (options != null && !CollectionUtils.isEmpty(options.getHeaders())) {
                options.getHeaders().forEach((key, value) -> {
                    if (StringUtils.hasText(key) && value != null) {
                        messageBuilder.setHeader(key, value);
                    }
                });
            }
            if (StringUtils.hasText(event.businessKey())) {
                messageBuilder.setHeader(MessageConst.PROPERTY_KEYS, event.businessKey());
            }
            Message<byte[]> message = messageBuilder.build();
            if (outboxStore.supportsLeaseRenewal() && !outboxStore.renew(claim)) {
                metrics.outbox("stale_claim");
                return;
            }
            SendResult result;
            if (StringUtils.hasText(shardingKey)) {
                result = rocketMQTemplate.syncSendOrderly(destination, message, shardingKey, timeout);
            } else if (delayLevel != null) {
                result = rocketMQTemplate.syncSend(destination, message, timeout, delayLevel);
            } else {
                result = rocketMQTemplate.syncSend(destination, message, timeout);
            }
            if (result == null || result.getSendStatus() != SendStatus.SEND_OK) {
                throw new MqException("Broker did not confirm Outbox message, status="
                        + (result == null ? "null" : result.getSendStatus()));
            }
        } catch (RuntimeException sendError) {
            recordSendFailure(claim, sendError);
            return;
        }

        try {
            outboxStore.markSent(claim);
            metrics.outbox("sent");
            log.debug("[mq-outbox] message sent. messageId={} topic={} tag={}",
                    event.messageId(), event.topic(), event.tag());
        } catch (RuntimeException ackError) {
            metrics.outbox("ack_failed");
            // Broker 已确认，但 DB ACK 不确定。不得反向标记失败，只能等待租约回收。
            log.error("[mq-outbox-ack-error] Broker confirmed, store ACK failed; "
                            + "lease recovery required. messageId={} topic={} exception={}",
                    event.messageId(), event.topic(), ackError.getClass().getName(), ackError);
        }
    }

    /**
     * 仅对 Broker 未确认或任务未准入的发送执行 token-scoped 失败更新。
     *
     * @param claim 当前认领身份
     * @param sendError 发送失败原因
     */
    private void recordSendFailure(MqOutboxClaim claim, RuntimeException sendError) {
        metrics.outbox("send_failed");
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
