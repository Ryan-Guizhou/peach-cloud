package com.peach.rocket.idempotent;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;
import org.springframework.util.StringUtils;

/**
 * 默认MQ幂等键解析器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class DefaultMqIdempotentKeyResolver implements MqIdempotentKeyResolver {

    @Override
    public String resolve(MqMessageEnvelope<?> envelope, MqConsumeContext context) {
        if (StringUtils.hasText(context.key())) {
            return context.topic() + ':' + context.tag() + ':' + context.key();
        }
        return context.topic() + ':' + context.messageId();
    }
}
