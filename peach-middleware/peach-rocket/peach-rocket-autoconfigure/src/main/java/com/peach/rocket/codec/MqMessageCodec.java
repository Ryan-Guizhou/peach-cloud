package com.peach.rocket.codec;

import com.peach.rocket.core.MqMessageEnvelope;

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
}
