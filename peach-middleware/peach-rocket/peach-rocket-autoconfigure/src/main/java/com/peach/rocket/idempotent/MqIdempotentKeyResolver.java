package com.peach.rocket.idempotent;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;

/**
 * MQ 幂等键解析器 SPI。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public interface MqIdempotentKeyResolver {

    /**
     * 解析幂等键。
     *
     * @param envelope 标准消息信封
     * @param context 消费上下文
     * @return 幂等键
     */
    String resolve(MqMessageEnvelope<?> envelope, MqConsumeContext context);
}
