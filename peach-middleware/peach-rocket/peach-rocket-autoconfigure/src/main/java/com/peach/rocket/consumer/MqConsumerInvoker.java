package com.peach.rocket.consumer;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.codec.MqMessageCodec;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.rocket.error.MqErrorHandler;
import com.peach.rocket.error.MqExceptionClassifier;
import com.peach.rocket.error.MqFailureAction;
import com.peach.rocket.exception.MqException;
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
 * MQ 消费调用器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
public class MqConsumerInvoker {

    private final MqMessageCodec codec;
    private final MqIdempotentStore idempotentStore;
    private final MqIdempotentKeyResolver idempotentKeyResolver;
    private final MqErrorHandler errorHandler;
    private final MqExceptionClassifier exceptionClassifier;
    private final PeachRocketProperties properties;

    public MqConsumerInvoker(MqMessageCodec codec, MqIdempotentStore idempotentStore,
                             MqIdempotentKeyResolver idempotentKeyResolver, MqErrorHandler errorHandler,
                             MqExceptionClassifier exceptionClassifier, PeachRocketProperties properties) {
        this.codec = codec;
        this.idempotentStore = idempotentStore;
        this.idempotentKeyResolver = idempotentKeyResolver;
        this.errorHandler = errorHandler;
        this.exceptionClassifier = exceptionClassifier;
        this.properties = properties;
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
                messageId == null ? envelope.getMessageId() : messageId,  // 消息唯一标识
                envelope.getTopic(),                                       // Topic
                envelope.getTag(),                                         // Tag
                envelope.getKey(),                                         // 业务 Key（用于路由）
                reconsumeTimes,                                            // 重试次数
                envelope.getHeaders()                                      // 自定义 Header
        );

        // ==================== 3. 幂等性处理 ====================
        // 3.1 解析幂等 Key（默认为 messageId，也可通过 SpEL 表达式自定义）
        String idempotentKey = idempotentKeyResolver.resolve(envelope, context);

        // 判断是否启用幂等：全局配置开启 && 消费者注解允许幂等
        boolean useIdempotent = properties.getConsumer().isEnableIdempotent() && consumer.idempotent();

        // 构建幂等上下文，包含存储幂等记录所需的所有信息
        MqIdempotentContext idempotentContext = new MqIdempotentContext(
                idempotentKey,
                RocketMqNaming.normalizeConsumerGroup(consumer.consumerGroup()),
                context.getTopic(),
                context.getTag(),
                context.getKey(),
                context.getMessageId(),
                properties.getConsumer().getIdempotentExpire()
        );

        // 3.2 幂等检查：如果该消息已经被成功消费过，直接返回，不重复执行业务逻辑
        if (useIdempotent && idempotentStore.isSuccess(idempotentContext)) {
            log.info("[mq-consume-idempotent-hit] consumer={} topic={} tag={} key={} messageId={}",
                    handlerName, context.getTopic(), context.getTag(),
                    context.getKey(), context.getMessageId());
            return;  // 幂等命中，直接返回
        }

        // 3.3 幂等抢占：尝试标记该消息正在处理中
        // 如果返回 false，说明有其他线程或实例正在处理该消息，抛出异常让 RocketMQ 稍后重试
        if (useIdempotent && !idempotentStore.tryStart(idempotentContext)) {
            throw new MqException("MQ message is already being processed, idempotentKey=" + idempotentKey);
        }

        // ==================== 4. 执行业务逻辑 ====================
        long start = System.currentTimeMillis();
        try {
            // 4.1 调用业务 Handler 处理消息
            // envelope.getPayload() 返回解码后的业务对象（如 OrderDTO）
            // context 包含消息元数据，业务方可根据需要进行使用
            handler.handle(envelope.getPayload(), context);

            // 4.2 处理成功：标记幂等记录为成功状态
            if (useIdempotent) {
                idempotentStore.markSuccess(idempotentContext);
            }

            // 4.3 记录成功消费日志（包含耗时）
            log.info("[mq-consume] consumer={} topic={} tag={} key={} messageId={} retry={} cost={}ms success=true",
                    handlerName, context.getTopic(), context.getTag(), context.getKey(),
                    context.getMessageId(), reconsumeTimes, System.currentTimeMillis() - start);

        } catch (RuntimeException ex) {
            // ==================== 5. 异常处理 ====================

            // 5.1 处理失败：标记幂等记录为失败状态
            // 这样后续该消息重试时，可以重新尝试处理
            if (useIdempotent) {
                idempotentStore.markFailed(idempotentContext);
            }

            // 5.2 调用错误处理器（可自定义扩展，如发送告警、记录死信等）
            errorHandler.handle(ex, envelope, context);

            // 5.3 异常分类：判断是否需要重试
            // 通过 MqExceptionClassifier 可自定义分类逻辑
            // 例如：网络超时可重试，参数校验失败不重试
            if (exceptionClassifier.classify(ex, envelope, context) == MqFailureAction.RETRY) {
                // 需要重试：直接抛出异常，RocketMQ 会按配置进行重试投递
                throw ex;
            }

            // 5.4 不需要重试：记录警告日志，消息消费成功（不抛出异常）
            // 这样消息会从 Broker 中确认删除，不会进入无限重试
            log.warn("[mq-consume-skip-retry] consumer={} topic={} tag={} key={} messageId={} exception={}",
                    handlerName, context.getTopic(), context.getTag(), context.getKey(),
                    context.getMessageId(), ex.getClass().getName());
            // 注意：这里不再抛出异常，相当于告诉 RocketMQ 消息已消费成功
            // 但业务上该消息实际处理失败了（适用于非关键业务或已记录告警的场景）
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
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Type raw = parameterizedType.getRawType();

                // 判断原始类型是否为 MqMessageHandler 或其子类
                if (raw instanceof Class && MqMessageHandler.class.isAssignableFrom((Class<?>) raw)) {
                    // 获取 MqMessageHandler 的第一个泛型参数（即 Payload 类型）
                    Type actualType = parameterizedType.getActualTypeArguments()[0];

                    // 如果泛型参数是具体的 Class，直接返回
                    if (actualType instanceof Class) {
                        return (Class<?>) actualType;
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
