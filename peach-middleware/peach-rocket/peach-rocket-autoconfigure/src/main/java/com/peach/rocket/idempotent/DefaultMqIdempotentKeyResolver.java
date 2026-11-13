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

    private final boolean legacy;

    public DefaultMqIdempotentKeyResolver() { this(false); }

    /** 旧键仅用于受控迁移；调用方负责双读或位点切换。 */
    public DefaultMqIdempotentKeyResolver(boolean legacy) { this.legacy = legacy; }

    @Override
    public String resolve(MqMessageEnvelope<?> envelope, MqConsumeContext context) {
        if (!legacy) {
            if (!StringUtils.hasText(envelope.messageId())) {
                throw new com.peach.rocket.exception.MqException("MQ stable event ID is required");
            }
            return "event:" + envelope.messageId();
        }
        if (StringUtils.hasText(context.key())) {
            return context.topic() + ':' + context.tag() + ':' + context.key();
        }
        return context.topic() + ':' + context.messageId();
    }
}
