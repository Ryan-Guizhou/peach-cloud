package com.peach.rocket.quickstart.config;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.annotation.MqEvent;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqDelay;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.rocket.core.MqPublisher;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.quickstart.event.OrderCreatedEvent;
import com.peach.rocket.quickstart.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 不连接 Broker 的 {@link MqPublisher}：记录发送并按 topic/tag 同步调用 {@link MqMessageHandler}。
 *
 * <p>用于 Quickstart 发布-消费闭环与顺序 {@code shardingKey} 契约验证。
 * 延迟与事务发送只记录结果，不模拟 Broker 两阶段或定时投递。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
public class InProcessMqPublisher implements MqPublisher {

    static final String RAW_STATUS = "IN_PROCESS";

    private final ApplicationContext applicationContext;
    private final List<Object> published = new CopyOnWriteArrayList<>();
    private final List<String> orderlyShardingKeys = new CopyOnWriteArrayList<>();

    public InProcessMqPublisher(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> MqSendResult publish(T payload) {
        return publishAndDeliver(payload, true);
    }

    @Override
    public <T> MqSendResult publish(T payload, MqSendOptions options) {
        return publish(payload);
    }

    @Override
    public <T> CompletableFuture<MqSendResult> publishAsync(T payload) {
        return CompletableFuture.completedFuture(publish(payload));
    }

    @Override
    public <T> CompletableFuture<MqSendResult> publishAsync(T payload, MqSendOptions options) {
        return publishAsync(payload);
    }

    @Override
    public <T> void publishOneWay(T payload, MqSendOptions options) {
        publish(payload);
    }

    /**
     * 顺序发送：要求稳定 shardingKey，并同步投递给匹配的消费者。
     */
    @Override
    public <T> MqSendResult publishOrderly(T payload, String shardingKey) {
        if (!StringUtils.hasText(shardingKey)) {
            throw new IllegalArgumentException("orderly publish requires a stable shardingKey");
        }
        orderlyShardingKeys.add(shardingKey);
        log.info("in-process orderly publish, shardingKey={}", shardingKey);
        return publishAndDeliver(payload, true);
    }

    @Override
    public <T> MqSendResult publishDelay(T payload, MqDelay delay) {
        log.info("in-process publisher records delay without timed delivery");
        return publishAndDeliver(payload, false);
    }

    @Override
    public <T> MqSendResult publishTransaction(T payload, String transactionKey) {
        return publishTransaction(payload, transactionKey, MqSendOptions.defaults());
    }

    @Override
    public <T> MqSendResult publishTransaction(T payload, String transactionKey, MqSendOptions options) {
        log.info("in-process publisher records transaction without two-phase commit");
        return publishAndDeliver(payload, false);
    }

    /**
     * @return 已记录的顺序分片键，按发送顺序
     */
    public List<String> orderlyShardingKeys() {
        return List.copyOf(orderlyShardingKeys);
    }

    /**
     * @return 已发布的业务消息快照
     */
    public List<Object> published() {
        return List.copyOf(published);
    }

    /**
     * 清空发送记录，便于测试隔离。
     */
    public void reset() {
        published.clear();
        orderlyShardingKeys.clear();
    }

    private <T> MqSendResult publishAndDeliver(T payload, boolean deliver) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        published.add(payload);
        MqEvent event = payload.getClass().getAnnotation(MqEvent.class);
        if (event == null) {
            throw new IllegalStateException("missing @MqEvent on " + payload.getClass().getName());
        }
        String messageId = UUID.randomUUID().toString();
        if (deliver) {
            deliver(payload, event, messageId);
        }
        return MqSendResult.builder()
                .success(true)
                .messageId(messageId)
                .topic(event.topic())
                .tag(event.tag())
                .key(businessKey(payload))
                .rawStatus(RAW_STATUS)
                .build();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void deliver(Object payload, MqEvent event, String messageId) {
        MqConsumeContext context = new MqConsumeContext(
                messageId, event.topic(), event.tag(), businessKey(payload), 0, Map.of());
        List<MqMessageHandler> matched = new ArrayList<>();
        for (MqMessageHandler handler : applicationContext.getBeansOfType(MqMessageHandler.class).values()) {
            MqConsumer consumer = AnnotationUtils.findAnnotation(AopUtils.getTargetClass(handler), MqConsumer.class);
            if (consumer == null) {
                continue;
            }
            if (event.topic().equals(consumer.topic()) && event.tag().equals(consumer.tag())) {
                handler.handle(payload, context);
                matched.add(handler);
            }
        }
        log.info("in-process delivered, topic={}, tag={}, handlers={}", event.topic(), event.tag(), matched.size());
    }

    private static String businessKey(Object payload) {
        if (payload instanceof OrderCreatedEvent created) {
            return String.valueOf(created.orderId());
        }
        if (payload instanceof OrderPaidEvent paid) {
            return String.valueOf(paid.orderId());
        }
        return payload.getClass().getSimpleName();
    }
}
