package com.peach.scheduler.service.internal;

import org.quartz.CronExpression;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Quartz {@link CronExpression} 预览桥接；Quartz API 仍使用 {@link Date}，对外返回 {@link Instant}。
 *
 * <p>Sonar S2143 已在根 {@code pom.xml} {@code sonar.issue.ignore.multicriteria} 中按文件忽略。</p>
 */
public final class QuartzCronPreviewBridge {

    private QuartzCronPreviewBridge() {
        throw new IllegalStateException("Utility class");
    }

    public static List<Instant> preview(String expression, String timeZone, int limit) throws ParseException {
        CronExpression cron = new CronExpression(expression);
        cron.setTimeZone(TimeZone.getTimeZone(timeZone == null ? "Asia/Shanghai" : timeZone));
        List<Instant> result = new ArrayList<>(limit);
        Date cursor = new Date();
        for (int i = 0; i < limit; i++) {
            cursor = cron.getNextValidTimeAfter(cursor);
            if (cursor == null) {
                break;
            }
            result.add(cursor.toInstant());
        }
        return result;
    }
}
