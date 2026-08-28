package com.peach.rocket.idempotent;

import java.time.Duration;

/**
 * MQ幂等上下文。
 * <p>该模型用于把一次消费在幂等判断阶段所需的关键信息收敛到统一对象中，便于不同幂等存储实现共享同一套
 * 输入语义，例如内存实现、JDBC 实现、Redis 实现或业务自定义实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public record MqIdempotentContext(
        String idempotentKey,
        String consumerGroup,
        String topic,
        String tag,
        String businessKey,
        String messageId,
        Duration expire) {
}
