package com.peach.rocket.core;

import com.peach.rocket.exception.MqException;
import java.time.Duration;
import java.util.List;

/**
 * RocketMQ 经典延迟级别解析，Publisher 与 Outbox 使用同一校验规则。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
public final class MqDelayLevels {

    private static final List<Duration> LEVELS = List.of(
            Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(10),
            Duration.ofSeconds(30), Duration.ofMinutes(1), Duration.ofMinutes(2),
            Duration.ofMinutes(3), Duration.ofMinutes(4), Duration.ofMinutes(5),
            Duration.ofMinutes(6), Duration.ofMinutes(7), Duration.ofMinutes(8),
            Duration.ofMinutes(9), Duration.ofMinutes(10), Duration.ofMinutes(20),
            Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2));

    private MqDelayLevels() {
    }

    /**
     * 校验延迟并映射到 Broker 支持的级别。
     *
     * @param delay 延迟参数，null 表示不延迟
     * @return RocketMQ 延迟级别，null 表示普通发送
     */
    public static Integer resolve(MqDelay delay) {
        if (delay == null) {
            return null;
        }
        if (delay.rocketMqDelayLevel() != null) {
            int level = delay.rocketMqDelayLevel();
            if (level < 1 || level > LEVELS.size()) {
                throw new MqException("Unsupported RocketMQ delay level: " + level);
            }
            return level;
        }
        Duration duration = delay.duration();
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new MqException("MQ delay duration must be positive");
        }
        if (duration.compareTo(LEVELS.get(LEVELS.size() - 1)) > 0) {
            throw new MqException("MQ delay duration exceeds the supported maximum: "
                    + LEVELS.get(LEVELS.size() - 1));
        }
        for (int i = 0; i < LEVELS.size(); i++) {
            if (LEVELS.get(i).compareTo(duration) >= 0) {
                return i + 1;
            }
        }
        throw new MqException("Unsupported RocketMQ delay duration: " + duration);
    }
}
