package com.peach.scheduler.quartz;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.provider.ScheduleTriggerHandler;
import org.quartz.Scheduler;
import org.springframework.beans.factory.InitializingBean;

/**
 * QuartzTrigger处理器注册器。
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
     * 创建实例。
     *
     * @param scheduler scheduler。
     * @param triggerHandler trigger Handler。
     */
    public QuartzTriggerHandlerRegistrar(Scheduler scheduler, ScheduleTriggerHandler triggerHandler) {
        this.scheduler = scheduler;
        this.triggerHandler = triggerHandler;
    }

    /**
     * 接口实现。
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        scheduler.getContext().put(AbstractPeachQuartzJob.CONTEXT_TRIGGER_HANDLER, triggerHandler);
    }
}
