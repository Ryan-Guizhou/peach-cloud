package com.peach.rocket.outbox;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.exception.MqException;
import java.util.ArrayList;
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
        if (properties.getOutbox().isParallelEnabled() && parallelExecutor == null) {
            throw new IllegalStateException("Outbox parallel mode requires a managed virtual-thread executor");
        }
        this.outboxStore = outboxStore;
        this.rocketMQTemplate = rocketMQTemplate;
        this.properties = properties;
        this.parallelExecutor = parallelExecutor;
    }

    @Scheduled(fixedDelayString = "${peach.rocket.outbox.scan-interval-ms:2000}")
    public void dispatch() {
        List<MqOutboxClaim> claims = outboxStore.claimPending(properties.getOutbox().getBatchSize());
        if (claims.isEmpty()) {
            return;
        }
        // 顺序分片键/延迟语义在本阶段沿用单线程投递，避免异步任务打乱同键消息。
        if (parallelExecutor == null || requiresSerialDelivery(claims)) {
            for (MqOutboxClaim claim : claims) {
                deliver(claim);
            }
            return;
        }
        List<Future<?>> pending = new ArrayList<>(claims.size());
        for (MqOutboxClaim claim : claims) {
            try {
                pending.add(parallelExecutor.submit(() -> deliver(claim)));
            } catch (RejectedExecutionException rejected) {
                // 任务未准入。必须通过 owner token 记录失败，不能丢弃或 ACK。
                recordSendFailure(claim, rejected);
            }
        }
        // 调度扫描要等待本批任务完结，不能在上一批尚未回写时继续批量认领。
        for (Future<?> task : pending) {
            try {
                task.get();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                log.warn("[mq-outbox] dispatch interrupted; unfinished claims will recover by lease");
                return;
            } catch (ExecutionException failure) {
                log.error("[mq-outbox] worker failed unexpectedly; claim lease recovery may be required",
                        failure.getCause());
            }
        }
    }

    private boolean requiresSerialDelivery(List<MqOutboxClaim> claims) {
        return claims.stream().anyMatch(claim -> {
            MqSendOptions options = claim.event().options();
            return options != null && ((options.getShardingKey() != null && !options.getShardingKey().isBlank())
                    || options.getDelay() != null);
        });
    }

    private void deliver(MqOutboxClaim claim) {
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
                return;
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
