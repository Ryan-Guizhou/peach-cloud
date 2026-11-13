package com.peach.rocket.codec;

import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.exception.MqException;
import java.lang.reflect.Type;

/**
 * MQ消息Codec接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public interface MqMessageCodec {

    /**
     * 编码标准消息信封。
     *
     * @param envelope 标准消息信封
     * @return 消息字节数组
     */
    byte[] encode(MqMessageEnvelope<?> envelope);

    /**
     * 解码标准消息信封。
     *
     * @param body 消息字节数组
     * @param payloadType payload 类型
     * @param <T> payload 类型
     * @return 标准消息信封
     */
    <T> MqMessageEnvelope<T> decode(byte[] body, Class<T> payloadType);

    /**
     * 支持泛型 Payload 的解码入口。旧 Codec 可继续实现 Class 版本；
     * 不认识参数化类型时明确失败，而不是静默退回原始 Map/List。
     *
     * @param body 信封字节
     * @param payloadType 带泛型参数的业务类型
     * @param <T> 业务类型
     * @return 消息信封
     */
    @SuppressWarnings("unchecked")
    default <T> MqMessageEnvelope<T> decode(byte[] body, Type payloadType) {
        if (payloadType instanceof Class<?> type) {
            return (MqMessageEnvelope<T>) decode(body, type);
        }
        throw new MqException("Codec does not support parameterized payload types");
    }
}
