package com.peach.rocket.idempotent;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;

/**
 * MQ幂等键解析器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
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
