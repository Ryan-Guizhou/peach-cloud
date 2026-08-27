package com.peach.scheduler.quartz.internal;

import java.time.Instant;
import java.util.Date;

/**
 * Quartz {@link java.util.Date} 边界桥接。
 *
 * <p>Quartz {@code TriggerBuilder.startAt} 等 API 仍要求 {@code Date}；调度模块对外使用
 * {@code Instant}，经本类转换。Sonar S2143 已在根 {@code pom.xml} 多条件忽略中配置。</p>
 */
public final class QuartzDateBridge {

    private QuartzDateBridge() {
        throw new IllegalStateException("Utility class");
    }

    public static Date toQuartzDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }
}
