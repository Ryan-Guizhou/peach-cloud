package com.peach.scheduler.provider;

/**
 * 调度触发回调。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ScheduleTriggerHandler {

    /**
     * 创建相关对象。
     *
     * @param context 参数说明
     */
    void onTrigger(ScheduleTriggerContext context);
}
