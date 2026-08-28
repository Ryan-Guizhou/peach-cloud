package com.peach.scheduler.provider;

/**
 * ScheduleTrigger处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ScheduleTriggerHandler {

    /**
     * 创建实例。
     *
     * @param context context。
     */
    void onTrigger(ScheduleTriggerContext context);
}
