package com.peach.rocket.consumer;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.rocket.context.MqTraceContextPropagator;
import com.peach.rocket.error.MqErrorHandler;
import com.peach.rocket.error.MqExceptionClassifier;
import com.peach.rocket.error.MqFailureAction;
import com.peach.rocket.error.MqFailureRecorder;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.idempotent.MqIdempotentClaim;
import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentKeyResolver;
import com.peach.rocket.idempotent.MqIdempotentStore;
import com.peach.rocket.support.RocketMqNaming;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;

/**
 * MQConsumer调用器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Slf4j
public class MqConsumerInvoker {

    private final MqMessageCodec codec;
    private final MqIdempotentStore idempotentStore;
    private final MqIdempotentKeyResolver idempotentKeyResolver;
    private final MqErrorHandler errorHandler;
    private final MqExceptionClassifier exceptionClassifier;
    private final MqFailureRecorder failureRecorder;
    private final PeachRocketProperties properties;
    private final MqTraceContextPropagator traceContextPropagator;

    public MqConsumerInvoker(MqMessageCodec codec, MqIdempotentStore idempotentStore,
                             MqIdempotentKeyResolver idempotentKeyResolver, MqErrorHandler errorHandler,
                             MqExceptionClassifier exceptionClassifier, PeachRocketProperties properties) {
        this(codec, idempotentStore, idempotentKeyResolver, errorHandler, exceptionClassifier, properties,
                MqTraceContextPropagator.NOOP);
    }

    /**
     * 创建使用空链路传播器的 MQ 消费调用器。
     *
     * @param codec 消息编解码器
     * @param idempotentStore 幂等存储
     * @param idempotentKeyResolver 幂等键解析器
     * @param errorHandler 消费异常处理器
     * @param exceptionClassifier 异常分类器
     * @param properties RocketMQ 配置
     */
    public MqConsumerInvoker(MqMessageCodec codec, MqIdempotentStore idempotentStore,
                             MqIdempotentKeyResolver idempotentKeyResolver, MqErrorHandler errorHandler,
                             MqExceptionClassifier exceptionClassifier, PeachRocketProperties properties,
                             MqTraceContextPropagator traceContextPropagator) {
        this(codec, idempotentStore, idempotentKeyResolver, errorHandler, exceptionClassifier, properties,
                traceContextPropagator, MqFailureRecorder.NOOP);
    }

    /**
     * 创建支持持久化失败回执的消费调用器。
     *
     * @param codec 消息编解码器
     * @param idempotentStore 幂等存储
     * @param idempotentKeyResolver 幂等键解析器
     * @param errorHandler 错误处理器
     * @param exceptionClassifier 错误分类器
     * @param properties RocketMQ 配置
     * @param traceContextPropagator Trace 传播器
     * @param failureRecorder 持久化失败回执
     */
    public MqConsumerInvoker(MqMessageCodec codec, MqIdempotentStore idempotentStore,
                             MqIdempotentKeyResolver idempotentKeyResolver, MqErrorHandler errorHandler,
                             MqExceptionClassifier exceptionClassifier, PeachRocketProperties properties,
                             MqTraceContextPropagator traceContextPropagator, MqFailureRecorder failureRecorder) {
        this.failureRecorder = failureRecorder == null ? MqFailureRecorder.NOOP : failureRecorder;
        this.codec = codec;
        this.idempotentStore = idempotentStore;
        this.idempotentKeyResolver = idempotentKeyResolver;
        this.errorHandler = errorHandler;
        this.exceptionClassifier = exceptionClassifier;
        this.properties = properties;
        this.traceContextPropagator = traceContextPropagator == null
                ? MqTraceContextPropagator.NOOP
                : traceContextPropagator;
    }

    /**
     * 消息消费核心调用器
     *
     * 该类负责将 RocketMQ 接收到的消息，经过解码、幂等校验、业务处理、异常分类等完整链路，
     * 最终交给业务 Handler 执行。是整个消费者端最核心的处理流程。
     *
     * @author Mr Shu
     * @version 1.0.0
     * @since 2026/6/26
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void invoke(MqMessageHandler handler, String handlerName, byte[] body,
                       MqConsumer consumer, String messageId, int reconsumeTimes) {

        // ==================== 1. 解析消息体类型 ====================
        // 通过反射解析 Handler 的泛型参数，确定 Payload 的实际类型（如 OrderDTO）
        Class<?> payloadType = resolvePayloadType(handler);

        // 解码消息体：将字节数组反序列化为 MqMessageEnvelope 对象
        // 信封中包含：业务数据（payload）、消息ID、Topic、Tag、Key、Headers 等
        MqMessageEnvelope<?> envelope = codec.decode(body, payloadType);

        // ==================== 2. 构建消费上下文 ====================
        // 统一使用信封中的 messageId（业务生成的 ID），若为空则使用 RocketMQ 的 messageId
        MqConsumeContext context = new MqConsumeContext(
                messageId == null ? envelope.messageId() : messageId,
                envelope.topic(),
                envelope.tag(),
                envelope.key(),
                reconsumeTimes,
                envelope.headers()
        );

        try (MqTraceContextPropagator.MqTraceScope traceScope = traceContextPropagator
                .startConsumerSpan(context.topic(), context.headers())) {

            // ==================== 3. 幂等性处理 ====================
            // 3.1 解析幂等 Key（默认为 messageId，也可通过 SpEL 表达式自定义）
            String idempotentKey = idempotentKeyResolver.resolve(envelope, context);

            // 判断是否启用幂等：全局配置开启 && 消费者注解允许幂等
            boolean useIdempotent = properties.getConsumer().isEnableIdempotent() && consumer.idempotent();

            // 构建幂等上下文，包含存储幂等记录所需的所有信息
            MqIdempotentContext idempotentContext = new MqIdempotentContext(
                    idempotentKey,
                    RocketMqNaming.normalizeConsumerGroup(consumer.consumerGroup(), properties),
                    context.topic(),
                    context.tag(),
                    context.key(),
                    context.messageId(),
                    properties.getConsumer().getIdempotentExpire()
            );

            // 3.2 原子认领消除独立 isSuccess 与 tryStart 之间的竞态窗口。
            MqIdempotentClaim idempotentClaim = null;
            if (useIdempotent) {
                idempotentClaim = idempotentStore.claim(idempotentContext);
                if (idempotentClaim.status() == MqIdempotentClaim.Status.SUCCEEDED) {
                    log.debug("[mq-consume-idempotent-hit] consumer={} topic={} messageId={}",
                            handlerName, context.topic(), context.messageId());
                    return;
                }
                if (idempotentClaim.status() != MqIdempotentClaim.Status.ACQUIRED) {
                    throw new MqException("MQ message is already being processed, idempotentKey=" + idempotentKey);
                }
            }

            // ==================== 4. 执行业务逻辑 ====================
            long start = System.currentTimeMillis();
            boolean handlerCompleted = false;
            try {
                // 4.1 调用业务 Handler 处理消息
                // envelope.getPayload() 返回解码后的业务对象（如 OrderDTO）
                // context 包含消息元数据，业务方可根据需要进行使用
                handler.handle(envelope.payload(), context);
                handlerCompleted = true;

                // 4.2 处理成功：标记幂等记录为成功状态
                if (useIdempotent) {
                    idempotentStore.markSuccess(idempotentContext, idempotentClaim);
                }

                // 4.3 记录成功消费日志（包含耗时）
                log.info("[mq-consume] consumer={} topic={} tag={} key={} messageId={} retry={} cost={}ms success=true",
                        handlerName, context.topic(), context.tag(), context.key(),
                        context.messageId(), reconsumeTimes, System.currentTimeMillis() - start);

            } catch (RuntimeException ex) {
                traceScope.error(ex);
                // ==================== 5. 异常处理 ====================

                // Handler 已成功但幂等 ACK 失败时不能释放认领，避免重复执行。
                boolean stateUpdated = true;
                if (useIdempotent && !handlerCompleted) {
                    try {
                        idempotentStore.markFailed(idempotentContext, idempotentClaim);
                    } catch (RuntimeException storeError) {
                        stateUpdated = false;
                        ex.addSuppressed(storeError);
                    }
                }
                try {
                    errorHandler.handle(ex, envelope, context);
                } catch (RuntimeException handlerError) {
                    ex.addSuppressed(handlerError);
                }
                if (handlerCompleted) {
                    throw ex;
                }

                MqFailureAction action;
                try {
                    action = exceptionClassifier.classify(ex, envelope, context);
                } catch (RuntimeException classifierError) {
                    ex.addSuppressed(classifierError);
                    throw ex;
                }
                if (action == MqFailureAction.SKIP && stateUpdated) {
                    boolean persisted = false;
                    try {
                        persisted = failureRecorder.record(ex, envelope, context);
                    } catch (RuntimeException recordError) {
                        ex.addSuppressed(recordError);
                    }
                    if (persisted) {
                        log.warn("[mq-consume-recorded-failure] consumer={} topic={} messageId={}",
                                handlerName, context.topic(), context.messageId());
                        return;
                    }
                }
                // 无持久化失败记录时一律 fail-closed，而不将消息 ACK 为成功。
                throw ex;
            }
        }
    }

    /**
     * 解析消息处理器（Handler）的泛型参数，获取 Payload 的实际类型
     *
     * 例如：MqMessageHandler<OrderDTO> 解析结果为 OrderDTO.class
     *
     * 实现原理：
     * <ol>
     *   <li>获取 Handler 的目标类（绕过 AOP 代理）</li>
     *   <li>遍历该类实现的所有接口</li>
     *   <li>找到 MqMessageHandler 接口的 ParameterizedType</li>
     *   <li>提取第一个泛型参数作为 Payload 类型</li>
     * </ol>
     *
     * 如果找不到泛型参数，默认返回 Map.class（兼容旧版本或非泛型写法）
     *
     * @param handler 消息处理器实例
     * @return Payload 的实际类型，默认 Map.class
     */
    private Class<?> resolvePayloadType(MqMessageHandler<?> handler) {
        // 获取目标类（绕过 Spring AOP 代理，获取真实的业务类）
        Class<?> targetClass = AopUtils.getTargetClass(handler);

        // 遍历该类实现的所有接口
        for (Type type : targetClass.getGenericInterfaces()) {
            // 判断接口是否带泛型参数（ParameterizedType）
            if (type instanceof ParameterizedType parameterizedType) {
                Type raw = parameterizedType.getRawType();

                // 判断原始类型是否为 MqMessageHandler 或其子类
                if (raw instanceof Class<?> rawClass && MqMessageHandler.class.isAssignableFrom(rawClass)) {
                    // 获取 MqMessageHandler 的第一个泛型参数（即 Payload 类型）
                    Type actualType = parameterizedType.getActualTypeArguments()[0];

                    // 如果泛型参数是具体的 Class，直接返回
                    if (actualType instanceof Class<?> actualClass) {
                        return actualClass;
                    }
                    // 注意：如果泛型参数也是泛型（如 List<Order>），这里会返回 null
                    // 当前实现不支持嵌套泛型，默认回退到 Map.class
                }
            }
        }

        // 未找到泛型参数时，默认使用 Map.class 进行解码
        // 这样兼容了非泛型写法或旧版本代码
        return Map.class;
    }
}
