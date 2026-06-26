package com.peach.rocket.core;

import java.time.Duration;

/**
 * 延迟消息参数。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqDelay {

    /**
     * RocketMQ 原生 delay level，优先级高于 duration。
     */
    private Integer rocketMqDelayLevel;

    /**
     * 期望延迟时长，会映射到最接近的 RocketMQ delay level。
     */
    private Duration duration;

    /**
     * 按 RocketMQ delay level 创建延迟参数。
     *
     * @param rocketMqDelayLevel RocketMQ delay level
     * @return 延迟参数
     */
    public static MqDelay level(int rocketMqDelayLevel) {
        MqDelay delay = new MqDelay();
        delay.setRocketMqDelayLevel(rocketMqDelayLevel);
        return delay;
    }

    /**
     * 按延迟时长创建延迟参数。
     *
     * @param duration 延迟时长
     * @return 延迟参数
     */
    public static MqDelay duration(Duration duration) {
        MqDelay delay = new MqDelay();
        delay.setDuration(duration);
        return delay;
    }

    public Integer getRocketMqDelayLevel() {
        return rocketMqDelayLevel;
    }

    public void setRocketMqDelayLevel(Integer rocketMqDelayLevel) {
        this.rocketMqDelayLevel = rocketMqDelayLevel;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }
}
