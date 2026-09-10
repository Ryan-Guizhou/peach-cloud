package com.peach.scheduler.quartz;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.provider.ScheduleTriggerHandler;
import org.quartz.Scheduler;
import org.springframework.beans.factory.InitializingBean;

/**
 * 将 Peach 调度触发处理器注册到 Quartz SchedulerContext。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class QuartzTriggerHandlerRegistrar implements InitializingBean {
    private final Scheduler scheduler;
    private final ScheduleTriggerHandler triggerHandler;

    /**
     * 创建 Quartz 触发处理器注册器。
     *
     * @param scheduler Quartz Scheduler。
     * @param triggerHandler Peach 调度触发处理器。
     */
    public QuartzTriggerHandlerRegistrar(Scheduler scheduler, ScheduleTriggerHandler triggerHandler) {
        this.scheduler = scheduler;
        this.triggerHandler = triggerHandler;
    }

    /**
     * 在属性设置完成后写入 Quartz SchedulerContext。
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        scheduler.getContext().put(AbstractPeachQuartzJob.CONTEXT_TRIGGER_HANDLER, triggerHandler);
    }
}
