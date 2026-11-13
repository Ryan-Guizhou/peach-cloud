package com.peach.rocket.idempotent;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.exception.MqException;
import org.springframework.util.StringUtils;

/**
 * 使用消息信封中的稳定事件 ID 生成消费幂等键。
 *
 * <p>不再回退到 Broker 投递 ID 或业务查询 Key，确保重试时获得相同的幂等标识。</p>
 */
public class DefaultMqIdempotentKeyResolver implements MqIdempotentKeyResolver {

    /**
     * 生成事件级幂等键。
     *
     * @param envelope 业务消息信封
     * @param context 当前消费上下文
     * @return 稳定事件幂等键
     */
    @Override
    public String resolve(MqMessageEnvelope<?> envelope, MqConsumeContext context) {
        if (envelope == null || !StringUtils.hasText(envelope.messageId())) {
            throw new MqException("MQ stable event ID is required");
        }
        return "event:" + envelope.messageId();
    }
}
