package com.peach.scheduler.service;

import org.springframework.stereotype.Indexed;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.quartz.CronExpression;

/**
 * 校验相关数据。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerCronService {
    /**
     * 创建相关对象。
     */
    public SchedulerCronService() {
    }

    /**
     * 获取相关数据。
     *
     * @param expression 参数说明
     * @param timeZone 参数说明
     * @param count 参数说明
     * @return 返回结果
     */
    public List<Date> preview(String expression, String timeZone, int count) {
        try {
            CronExpression cron = new CronExpression(expression);
            cron.setTimeZone(TimeZone.getTimeZone(timeZone == null ? "Asia/Shanghai" : timeZone));
            int limit = Math.max(1, Math.min(count, 20));
            List<Date> result = new ArrayList<Date>(limit);
            Date cursor = new Date();
            for (int i = 0; i < limit; i++) {
                cursor = cron.getNextValidTimeAfter(cursor);
                if (cursor == null) break;
                result.add(cursor);
            }
            return result;
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Invalid Quartz cron expression", ex);
        }
    }
}
