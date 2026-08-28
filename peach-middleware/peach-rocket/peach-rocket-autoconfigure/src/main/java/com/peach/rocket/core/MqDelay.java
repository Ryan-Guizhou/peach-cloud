package com.peach.rocket.core;

import java.time.Duration;

/**
 * MQ延迟值对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public record MqDelay(Integer rocketMqDelayLevel, Duration duration) {

    /**
     * 按 RocketMQ delay level 创建延迟参数。
     *
     * @param rocketMqDelayLevel RocketMQ delay level
     * @return 延迟参数
     */
    public static MqDelay level(int rocketMqDelayLevel) {
        return new MqDelay(rocketMqDelayLevel, null);
    }

    /**
     * 按延迟时长创建延迟参数。
     *
     * @param duration 延迟时长
     * @return 延迟参数
     */
    public static MqDelay duration(Duration duration) {
        return new MqDelay(null, duration);
    }
}
