package com.peach.rocket.producer;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.context.DefaultMqHeaderResolver;
import com.peach.rocket.exception.MqException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * MQ 发送准入：限制编码体、Header 与框架内部事务属性的资源预算。
 *
 * <p>业务 Header 必须拒绝 SDK 保留字段；事务 Key 只能通过专用的框架入口计入预算，
 * 不得作为调用方传入的业务 Header 伪造。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
public final class MqProducerMessageGuard {

    private static final String TRANSACTION_KEY_HEADER = "PEACH_ROCKET_TRANSACTION_KEY";

    private MqProducerMessageGuard() {
    }

    /**
     * 验证普通消息的序列化字节、Header 项数及 UTF-8 字节预算。
     *
     * @param body 编码后的消息信封
     * @param headers 业务及 Trace Header
     * @param businessKey 业务消息 Key
     * @param limits Producer 资源限制
     */
    public static void validate(byte[] body, Map<String, String> headers, String businessKey,
                                PeachRocketProperties.Producer limits) {
        validateInternal(body, headers, businessKey, null, limits);
    }

    /**
     * 验证事务半消息并将框架生成的事务 Key 计入预算；业务 Header 仍禁止携带保留字段。
     *
     * @param body 编码后的消息信封
     * @param headers 已经校验过来源的业务与 Trace Header
     * @param businessKey 业务消息 Key
     * @param transactionKey 框架生成/消费的事务追踪键
     * @param limits Producer 资源限制
     */
    public static void validateTransaction(byte[] body, Map<String, String> headers, String businessKey,
                                           String transactionKey, PeachRocketProperties.Producer limits) {
        if (!StringUtils.hasText(transactionKey)) {
            throw new MqException("RocketMQ transaction key must not be blank");
        }
        validateInternal(body, headers, businessKey, transactionKey, limits);
    }

    private static void validateInternal(byte[] body, Map<String, String> headers, String businessKey,
                                         String transactionKey, PeachRocketProperties.Producer limits) {
        if (body == null || body.length > limits.getMaxPayloadBytes()) {
            throw new MqException("RocketMQ encoded payload exceeds configured limit");
        }
        int count = CollectionUtils.isEmpty(headers) ? 0 : headers.size();
        long bytes = 0L;
        if (!CollectionUtils.isEmpty(headers)) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                DefaultMqHeaderResolver.validateBusinessHeader(header.getKey(), header.getValue());
                bytes += utf8Bytes(header.getKey()) + utf8Bytes(header.getValue());
                if (bytes > limits.getMaxHeaderBytes()) {
                    throw new MqException("RocketMQ header bytes exceed configured limit");
                }
            }
        }
        if (StringUtils.hasText(businessKey)) {
            count++;
            bytes += utf8Bytes(MessageConst.PROPERTY_KEYS) + utf8Bytes(businessKey);
        }
        if (transactionKey != null) {
            count++;
            bytes += utf8Bytes(TRANSACTION_KEY_HEADER) + utf8Bytes(transactionKey);
        }
        if (count > limits.getMaxHeaderCount()) {
            throw new MqException("RocketMQ header count exceeds configured limit");
        }
        if (bytes > limits.getMaxHeaderBytes()) {
            throw new MqException("RocketMQ header bytes exceed configured limit");
        }
    }

    private static int utf8Bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }
}
