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
 * 发送前检查已序列化消息大小与业务 Header 预算，防止超大报文进入 SDK。
 *
 * <p>不得记录 body 或 Header 原始值，以避免敏感数据泄露。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
public final class MqProducerMessageGuard {
    private MqProducerMessageGuard() {
    }

    /**
     * 验证编码后字节长度、Header 项数及 UTF-8 总字节数。
     *
     * @param body 已编码消息信封
     * @param headers 经链路传播后的 Header
     * @param businessKey 本次发送的业务键
     * @param limits Producer 限额配置
     */
    public static void validate(byte[] body, Map<String, String> headers, String businessKey,
                                PeachRocketProperties.Producer limits) {
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
