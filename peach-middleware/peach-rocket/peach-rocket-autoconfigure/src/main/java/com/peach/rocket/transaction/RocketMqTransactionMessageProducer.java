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
import com.peach.rocket.route.MqRoute;
import com.peach.rocket.route.MqRouteResolver;
import com.peach.rocket.support.RocketMqNaming;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * RocketMQMQ事务消息生产者。
 * <p>该类封装了 RocketMQ 事务消息的发送、本地事务执行和事务状态回查的完整流程。
 * 事务消息保证消息发送与本地事务的原子性，适用于订单支付、库存扣减等需要强一致性的场景。
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
    private TransactionMQProducer producer;

    /**
     * 生产者运行状态标识。
     */
    private volatile boolean running;

    public RocketMqTransactionMessageProducer(RocketMQProperties rocketMQProperties,
                                              PeachRocketProperties properties,
                                              MqMessageCodec codec,
                                              MqRouteResolver routeResolver,
                                              DefaultMqHeaderResolver headerResolver,
                                              ObjectProvider<MqTransactionHandler<?>> transactionHandlers) {
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

        // 检查生产者是否已启动
        if (!running || producer == null) {
            throw new MqException("RocketMQ transaction producer is not running");
        }

        // 构建发送选项（若无则使用默认值）
        MqSendOptions actualOptions = options == null ? MqSendOptions.defaults() : options;

        // 解析消息路由
        MqRoute route = routeResolver.resolve(payload, actualOptions);

        // 构建消息信封
        MqMessageEnvelope<T> envelope = buildEnvelope(payload, actualOptions, route);

        // 构建 RocketMQ 消息对象
        Message message = new Message(route.topic(), route.tag(), route.key(), codec.encode(envelope));

        // 设置事务 Key 到消息用户属性
        message.putUserProperty(PROPERTY_TRANSACTION_KEY, transactionKey);

        // 设置业务 Key（用于消息查询）
        if (StringUtils.hasText(route.key())) {
            message.putUserProperty(MessageConst.PROPERTY_KEYS, route.key());
        }

        // 构建事务参数对象，传递给 TransactionListener
        TransactionArgument argument = new TransactionArgument(transactionKey, route, payload);

        try {
            // 发送事务消息
            SendResult result = producer.sendMessageInTransaction(message, argument);

            log.info("[mq-send] mode=transaction topic={} tag={} key={} transactionKey={} messageId={} status={} success={}",
                    route.topic(), route.tag(), route.key(), transactionKey,
                    result == null ? null : result.getMsgId(),
                    result == null || result.getSendStatus() == null ? null : result.getSendStatus().name(),
                    isSendOk(result));

            return MqSendResult.builder()
                    .success(isSendOk(result))
                    .messageId(result == null ? null : result.getMsgId())
                    .topic(route.topic())
                    .tag(route.tag())
                    .key(route.key())
                    .rawStatus(result == null || result.getSendStatus() == null ? null : result.getSendStatus().name())
                    .build();

        } catch (Exception ex) {
            throw new MqException("Failed to send RocketMQ transaction message", ex);
        }
    }

    @Override
    public void start() {
        // 已启动或无处理器时跳过
        if (running || handlers.isEmpty()) {
            return;
        }

        try {
            // 创建事务消息生产者
            producer = new TransactionMQProducer(properties.getTransaction().getProducerGroup());
            producer.setNamesrvAddr(rocketMQProperties.getNameServer());
            // 设置事务监听器（委托给 DelegatingTransactionListener）
            producer.setTransactionListener(new DelegatingTransactionListener());
            producer.start();
            running = true;

            log.info("[mq-transaction] producer started. group={} nameServer={} handlerCount={}",
                    properties.getTransaction().getProducerGroup(),
                    rocketMQProperties.getNameServer(),
                    handlers.size());

        } catch (Exception ex) {
            throw new MqException("Failed to start RocketMQ transaction producer", ex);
        }
    }

    @Override
    public void stop() {
        if (producer != null) {
            producer.shutdown();
        }
        running = false;
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
            TransactionArgument argument = (TransactionArgument) arg;
            TransactionHandlerAdapter adapter = findHandler(argument.route().topic(),
                    argument.route().tag());

            if (adapter == null) {
                log.warn("[mq-transaction] no handler found on execute. topic={} tag={}",
                        argument.route().topic(), argument.route().tag());
                return LocalTransactionState.UNKNOW;
            }

            return toRocketState(adapter.execute(argument.payload(), argument.transactionKey()));
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
            // 从消息属性中获取事务 Key
            String transactionKey = message.getUserProperty(PROPERTY_TRANSACTION_KEY);

            // 查找匹配的处理器
            TransactionHandlerAdapter adapter = findHandler(message.getTopic(), message.getTags());

            if (adapter == null) {
                log.warn("[mq-transaction] no handler found on check. topic={} tag={} transactionKey={}",
                        message.getTopic(), message.getTags(), transactionKey);
                return LocalTransactionState.UNKNOW;
            }

            // 解码消息体
            MqMessageEnvelope<?> envelope = codec.decode(message.getBody(), adapter.getPayloadType());

            // 执行回查
            return toRocketState(adapter.check(envelope.payload(), transactionKey));
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
        private final Class<?> payloadType;

        private TransactionHandlerAdapter(MqTransactionHandler handler, Class<?> payloadType) {
            this.handler = handler;
            this.payloadType = payloadType;
        }

        /**
         * 执行本地事务。
         * @param payload
         * @param transactionKey
         * @return
         */
        private MqLocalTransactionState execute(Object payload, String transactionKey) {
            return handler.executeLocalTransaction(payload, transactionKey);
        }

        /**
         * 回查本地事务状态。
         * @param payload
         * @param transactionKey
         * @return
         */
        private MqLocalTransactionState check(Object payload, String transactionKey) {
            return handler.checkLocalTransaction(payload, transactionKey);
        }

        private Class<?> getPayloadType() {
            return payloadType;
        }
    }
}