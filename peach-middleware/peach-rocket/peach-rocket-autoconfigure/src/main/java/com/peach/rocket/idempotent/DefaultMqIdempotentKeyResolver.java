package com.peach.rocket.idempotent;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageEnvelope;
import org.springframework.util.StringUtils;

/**
 * 默认 MQ 幂等键解析器。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class DefaultMqIdempotentKeyResolver implements MqIdempotentKeyResolver {

    @Override
    public String resolve(MqMessageEnvelope<?> envelope, MqConsumeContext context) {
        if (StringUtils.hasText(context.getKey())) {
            return context.getTopic() + ':' + context.getTag() + ':' + context.getKey();
        }
        return context.getTopic() + ':' + context.getMessageId();
    }
}
