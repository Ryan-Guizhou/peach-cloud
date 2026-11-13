package com.peach.rocket.transaction;

import java.time.ZoneId;

import com.peach.rocket.annotation.MqEvent;
import com.peach.rocket.annotation.MqTransaction;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.core.MqLocalTransactionState;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.core.MqSendResult;
import com.peach.rocket.core.MqTransactionHandler;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.producer.MqProducerAdmission;
import com.peach.rocket.producer.MqProducerMessageGuard;
import com.peach.rocket.route.MqRoute;
import com.peach.rocket.route.MqRouteResolver;
import com.peach.rocket.support.RocketMqNaming;
import com.peach.rocket.support.MqClientSecurity;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * RocketMQ 事务消息生产者，负责半消息、本地事务和 Broker 状态回查。
 * <p>该类封装了 RocketMQ 事务消息的发送、本地事务执行和事务状态回查的完整流程。
 * 事务消息通过 Broker 回查实现最终一致性，不代替业务数据库的本地事务与持久化幂等。
 * <p><b>核心流程：</b>
 * <ol>
 * <li>发送半消息（Half Message）到 Broker</li>
 * <li>执行本地事务（{@link MqTransactionHandler#executeLocalTransaction}）</li>
 * <li>根据本地事务结果提交或回滚消息</li>
 * <li>若本地事务执行超时，Broker 会回调 {@link MqTransactionHandler#checkLocalTransaction} 进行状态回查</li>
 * </ol>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Slf4j
public class RocketMqTransactionMessageProducer implements SmartLifecycle {

    /**
     * 消息属性 Key：事务标识。
     * 存储在消息的用户属性中，用于在回查时关联对应的事务。
     */
    public static final String PROPERTY_TRANSACTION_KEY = "PEACH_ROCKET_TRANSACTION_KEY";

    /**
     * RocketMQ 自动配置属性，用于获取 NameServer 地址。
     */
    private final RocketMQProperties rocketMQProperties;

    /**
     * Peach RocketMQ 自定义配置属性，包含事务消息相关配置。
     */
    private final PeachRocketProperties properties;

    /**
     * 消息编解码器，用于序列化/反序列化消息体。
     */
    private final MqMessageCodec codec;

    /**
     * 路由解析器，用于解析消息的目标 Topic 和 Tag。
     */
    private final MqRouteResolver routeResolver;

    /**
     * Header 解析器，用于解析消息的自定义头部信息。
     */
    private final DefaultMqHeaderResolver headerResolver;

    /**
     * 事务处理器映射表。
     * Key 格式：topic||tag（tag 为空时使用 *）
     * Value：事务处理器适配器
     */
    private final Map<String, TransactionHandlerAdapter> handlers = new HashMap<String, TransactionHandlerAdapter>();

    /**
     * RocketMQ 事务消息生产者实例。
     */
    private volatile TransactionMQProducer producer;

    /**
     * 生产者运行状态标识。
     */
    private volatile boolean running;

    /** 仅保护启动/停止，发送线程不持有生命周期锁。 */
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    /** 独立事务 Producer 的本地有界发送预算。 */
    private final MqProducerAdmission admission;

    public RocketMqTransactionMessageProducer(RocketMQProperties rocketMQProperties,
                                              PeachRocketProperties properties,
                                              MqMessageCodec codec,
                                              MqRouteResolver routeResolver,
                                              DefaultMqHeaderResolver headerResolver,
                                              ObjectProvider<MqTransactionHandler<?>> transactionHandlers) {
        this.admission = new MqProducerAdmission(properties.getProducer().getMaxInflight(),
                properties.getProducer().getInflightAcquireTimeout());
        this.rocketMQProperties = rocketMQProperties;
        this.properties = properties;
        this.codec = codec;
        this.routeResolver = routeResolver;
        this.headerResolver = headerResolver;
        // 按顺序注册所有事务处理器
        transactionHandlers.orderedStream().forEach(this::registerHandler);
    }

    /**
     * 发送事务消息。
     *
     * <p>执行流程：
     * <ol>
     *   <li>校验事务 Key 不能为空</li>
     *   <li>解析消息路由（Topic、Tag）</li>
     *   <li>构建消息信封并序列化</li>
     *   <li>设置事务 Key 到消息属性</li>
     *   <li>通过 RocketMQ 发送事务消息</li>
     * </ol>
     *
     * @param payload        消息体，可以是任意对象
     * @param transactionKey 事务唯一标识，用于关联本地事务和消息
     * @param options        发送选项，可为空
     * @param <T>            消息体类型
     * @return 发送结果
     * @throws MqException 发送失败时抛出
     */
    public <T> MqSendResult sendTransaction(T payload, String transactionKey, MqSendOptions options) {
        // 校验事务 Key
        if (!StringUtils.hasText(transactionKey)) {
            throw new MqException("transactionKey must not be blank for transaction message");
        }

        // 读取当前客户端快照，停机并发关闭时按 SDK 异常处理，不可能误用下一次启动的实例。
        TransactionMQProducer current = producer;
        if (!running || current == null) {
            throw new MqException("RocketMQ transaction producer is not running");
        }

        MqSendOptions actualOptions = MqSendOptions.copyOf(options);
        if (StringUtils.hasText(actualOptions.getShardingKey()) || actualOptions.getDelay() != null) {
            throw new MqException("Transaction send does not support shardingKey or delay options");
        }

        // 解析消息路由
        MqRoute route = routeResolver.resolve(payload, actualOptions);
        if (!handlers.containsKey(handlerKey(route.topic(), route.tag()))
                && !handlers.containsKey(handlerKey(route.topic(), "*"))) {
            throw new MqException("No local transaction handler matches the message route");
        }
        if (actualOptions.getTimeoutMillis() > 0
                && actualOptions.getTimeoutMillis() != properties.getProducer().getDefaultTimeout().toMillis()) {
            throw new MqException("Per-message transaction timeout is unsupported; configure producer.default-timeout");
        }

        // 构建消息信封
        MqMessageEnvelope<T> envelope = buildEnvelope(payload, actualOptions, route);

        // 对编码后的信封和包括事务 Key 的 Header 统一做预算限制。
        byte[] body = codec.encode(envelope);
        MqProducerMessageGuard.validateTransaction(
                body, envelope.headers(), route.key(), transactionKey, properties.getProducer());
        Message message = new Message(route.topic(), route.tag(), route.key(), body);

        // 设置事务 Key 到消息用户属性
        message.putUserProperty(PROPERTY_TRANSACTION_KEY, transactionKey);

        // 设置业务 Key（用于消息查询）
        if (StringUtils.hasText(route.key())) {
            message.setKeys(route.key());
        }

        // 构建事务参数对象，传递给 TransactionListener
        TransactionArgument argument = new TransactionArgument(transactionKey, route, payload);

        try {
            // 事务半消息发送在 Broker 同步调用返回前占用额度；不记录事务 Key 内容。
            SendResult result;
            try (MqProducerAdmission.Permit permit = admission.acquire("transaction")) {
                result = current.sendMessageInTransaction(message, argument);
            }

            log.info("[mq-send] mode=transaction topic={} tag={} messageId={} status={} success={}",
                    route.topic(), route.tag(),
                    result == null ? null : result.getMsgId(),
                    result == null || result.getSendStatus() == null ? null : result.getSendStatus().name(),
                    isSendOk(result));

            return MqSendResult.builder()
                    .success(isSendOk(result) && result instanceof TransactionSendResult transaction
                            && transaction.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE)
                    .messageId(result == null ? null : result.getMsgId())
                    .topic(route.topic())
                    .tag(route.tag())
                    .key(route.key())
                    .rawStatus(result == null || result.getSendStatus() == null ? null : result.getSendStatus().name()
                            + "/" + (result instanceof TransactionSendResult transaction
                            ? transaction.getLocalTransactionState() : "UNKNOWN"))
                    .build();

        } catch (Exception ex) {
            log.error("[mq-transaction-send-error] topic={} tag={} errorType={}",
                    route.topic(), route.tag(), ex.getClass().getName());
            if (ex instanceof MqException mqException) {
                throw mqException;
            }
            throw new MqException("Failed to send RocketMQ transaction message");
        }
    }

    /**
     * 在生命周期锁中启动事务 Producer，启动失败必须关闭已分配的 SDK 资源。
     */
    @Override
    public void start() {
        lifecycleLock.lock();
        try {
            if (running || handlers.isEmpty()) {
                return;
            }
            TransactionMQProducer candidate = null;
            try {
                candidate = createProducer(properties.getTransaction().getProducerGroup());
                candidate.setNamesrvAddr(rocketMQProperties.getNameServer());
                MqClientSecurity.configureClient(candidate, rocketMQProperties, properties);
                candidate.setSendMsgTimeout(Math.toIntExact(properties.getProducer().getDefaultTimeout().toMillis()));
                candidate.setTransactionListener(new DelegatingTransactionListener());
                candidate.start();
                admission.open();
                producer = candidate;
                running = true;
                log.info("[mq-transaction] producer started. group={} handlers={}",
                        properties.getTransaction().getProducerGroup(), handlers.size());
            } catch (Exception ex) {
                running = false;
                producer = null;
                if (candidate != null) {
                    try {
                        candidate.shutdown();
                    } catch (RuntimeException cleanupError) {
                        ex.addSuppressed(cleanupError);
                        log.warn("[mq-transaction] startup cleanup failed. group={} errorType={}",
                                properties.getTransaction().getProducerGroup(), cleanupError.getClass().getName(),
                                cleanupError);
                    }
                }
                log.error("[mq-transaction] producer start failed. group={} errorType={}",
                        properties.getTransaction().getProducerGroup(), ex.getClass().getName());
                throw new MqException("Failed to start RocketMQ transaction producer");
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 创建 SDK Producer；允许在无 Broker 测试中使用模拟实现验证启动故障恢复。
     *
     * @param group 事务生产者组
     * @return 未启动的 SDK 实例
     */
    protected TransactionMQProducer createProducer(String group) {
        return new TransactionMQProducer(group,
                MqClientSecurity.rpcHook(rocketMQProperties, properties));
    }

    /**
     * 关闭客户端时先清理可见状态；单次 shutdown 故障不会保留失效 Producer。
     */
    @Override
    public void stop() {
        lifecycleLock.lock();
        try {
            TransactionMQProducer current = producer;
            producer = null;
            running = false;
            if (!admission.closeAndDrain(properties.getConsumer().getShutdownDrainTimeout())) {
                log.warn("[mq-transaction] shutdown drain timed out; broker checks must recover uncertain results");
            }
            if (current == null) {
                return;
            }
            try {
                current.shutdown();
                log.info("[mq-transaction] producer stopped. group={}",
                        properties.getTransaction().getProducerGroup());
            } catch (RuntimeException ex) {
                log.error("[mq-transaction] producer shutdown failed. group={} errorType={}",
                        properties.getTransaction().getProducerGroup(), ex.getClass().getName());
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 指定启动阶段。
     * 设置为 {@code Integer.MAX_VALUE - 100}，确保在 RocketMQ 自动配置之后启动。
     *
     * @return 启动阶段
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    /**
     * 注册事务处理器。
     * 从处理器类中解析 @MqTransaction 注解，提取 Topic 和 Tag 作为 Key。
     *
     * @param handler 事务处理器实例
     */
    private void registerHandler(MqTransactionHandler<?> handler) {
        // 获取目标类（绕过 AOP 代理）
        Class<?> targetClass = AopUtils.getTargetClass(handler);

        // 查找 @MqTransaction 注解
        MqTransaction annotation = AnnotationUtils.findAnnotation(targetClass, MqTransaction.class);
        if (annotation == null) {
            return;
        }

        // 标准化 Topic 和 Tag
        String topic = RocketMqNaming.normalizeTopic(annotation.topic(), properties);
        String tag = annotation.tag();
        String key = handlerKey(topic, tag);

        // 检查是否重复注册
        if (handlers.containsKey(key)) {
            throw new MqException("Duplicate MQ transaction handler for topic/tag, topic=" + topic + ", tag=" + tag);
        }

        // 注册处理器适配器
        handlers.put(key, new TransactionHandlerAdapter(handler, resolvePayloadType(handler)));

        log.info("[mq-transaction] handler registered. topic={} tag={} handler={}",
                topic, tag, targetClass.getName());
    }


    /**
     * 构建消息信封。
     * 包含消息 ID、路由信息、消息体、Headers、时间戳等。
     *
     * @param payload 消息体
     * @param options 发送选项
     * @param route   路由信息
     * @param <T>     消息体类型
     * @return 消息信封
     */
    private <T> MqMessageEnvelope<T> buildEnvelope(T payload, MqSendOptions options, MqRoute route) {
        return MqMessageEnvelope.create(
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
    }

    /**
     * 解析消息版本号。
     * 从 @MqEvent 注解中读取，默认为 1。
     *
     * @param payload 消息体
     * @return 版本号
     */
    private int resolveVersion(Object payload) {
        MqEvent event = payload.getClass().getAnnotation(MqEvent.class);
        return event == null ? 1 : event.version();
    }

    /**
     * Delegating事务监听器。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private final class DelegatingTransactionListener implements TransactionListener {

        private TransactionHandlerAdapter findHandler(String topic, String tag) {
            TransactionHandlerAdapter exact = handlers.get(handlerKey(topic, tag));
            return exact == null ? handlers.get(handlerKey(topic, "*")) : exact;
        }

        private LocalTransactionState toRocketState(MqLocalTransactionState state) {
            if (state == MqLocalTransactionState.COMMIT) {
                return LocalTransactionState.COMMIT_MESSAGE;
            }
            if (state == MqLocalTransactionState.ROLLBACK) {
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }
            return LocalTransactionState.UNKNOW;
        }

        /**
         * 执行本地事务。
         * 由 RocketMQ 在半消息发送成功后回调。
         *
         * @param message RocketMQ 消息
         * @param arg     事务参数（TransactionArgument）
         * @return 本地事务执行结果
         */
        @Override
        public LocalTransactionState executeLocalTransaction(Message message, Object arg) {
            try {
                TransactionArgument argument = (TransactionArgument) arg;
                TransactionHandlerAdapter adapter = findHandler(argument.route().topic(), argument.route().tag());
                if (adapter == null) {
                    log.warn("[mq-transaction] local transaction handler missing. topic={} tag={}",
                            argument.route().topic(), argument.route().tag());
                    return LocalTransactionState.UNKNOW;
                }
                return toRocketState(adapter.execute(argument.payload(), argument.transactionKey()));
            } catch (RuntimeException ex) {
                // 不确定的业务结果必须交 Broker 回查，不能擅自判定提交或回滚。
                log.error("[mq-transaction] local execution failed. topic={} errorType={}",
                        message.getTopic(), ex.getClass().getName());
                return LocalTransactionState.UNKNOW;
            }
        }

        /**
         * 回查本地事务状态。
         * 当执行本地事务超时时，Broker 会回调此方法查询事务状态。
         *
         * @param message RocketMQ 消息扩展对象
         * @return 本地事务状态
         */
        @Override
        public LocalTransactionState checkLocalTransaction(MessageExt message) {
            try {
                String transactionKey = message.getUserProperty(PROPERTY_TRANSACTION_KEY);
                TransactionHandlerAdapter adapter = findHandler(message.getTopic(), message.getTags());
                if (adapter == null) {
                    log.warn("[mq-transaction] transaction check handler missing. topic={} tag={}",
                            message.getTopic(), message.getTags());
                    return LocalTransactionState.UNKNOW;
                }
                MqMessageEnvelope<?> envelope = codec.decode(message.getBody(), adapter.getPayloadType());
                return toRocketState(adapter.check(envelope.payload(), transactionKey));
            } catch (RuntimeException ex) {
                log.error("[mq-transaction] transaction check failed. topic={} messageId={} errorType={}",
                        message.getTopic(), message.getMsgId(), ex.getClass().getName());
                return LocalTransactionState.UNKNOW;
            }
        }
    }

    /**
     * 构建处理器 Key。
     * 格式：topic||tag，tag 为空时使用 *。
     */
    private String handlerKey(String topic, String tag) {
        return topic + "||" + (StringUtils.hasText(tag) ? tag : "*");
    }

    private Class<?> resolvePayloadType(MqTransactionHandler<?> handler) {
        ResolvableType type = ResolvableType.forClass(AopUtils.getTargetClass(handler))
                .as(MqTransactionHandler.class);
        Class<?> resolved = type.getGeneric(0).resolve();
        return resolved == null ? Object.class : resolved;
    }

    private boolean isSendOk(SendResult result) {
        return result != null && result.getSendStatus() == SendStatus.SEND_OK;
    }

    /**
     * 事务Argument值对象。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private record TransactionArgument(String transactionKey, MqRoute route, Object payload) {
    }

    /**
     * 事务处理器适配器。
     * 包装 MqTransactionHandler，缓存 Payload 类型和路由信息。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    private static final class TransactionHandlerAdapter {

        /** 原始事务处理器 */
        private final MqTransactionHandler handler;

        /** Payload 类型 */
        @Getter
        private final Class<?> payloadType;

        private TransactionHandlerAdapter(MqTransactionHandler handler, Class<?> payloadType) {
            this.handler = handler;
            this.payloadType = payloadType;
        }

        /**
         * 执行当前半消息关联的本地事务。
         *
         * @param payload 已解码的业务对象
         * @param transactionKey 事务唯一标识
         * @return 业务事务状态
         */
        private MqLocalTransactionState execute(Object payload, String transactionKey) {
            return handler.executeLocalTransaction(payload, transactionKey);
        }

        /**
         * Broker 回查期间查询业务事务的最终结果。
         *
         * @param payload 已解码的业务对象
         * @param transactionKey 事务唯一标识
         * @return 业务事务状态
         */
        private MqLocalTransactionState check(Object payload, String transactionKey) {
            return handler.checkLocalTransaction(payload, transactionKey);
        }

     }
}
