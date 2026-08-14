package com.peach.scheduler.quartz;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.provider.ScheduleTriggerHandler;
import org.quartz.Scheduler;
import org.springframework.beans.factory.InitializingBean;

/**
 * 调度模块相关说明。
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
     * 创建相关对象。
     *
     * @param scheduler 参数说明
     * @param triggerHandler 参数说明
     */
    public QuartzTriggerHandlerRegistrar(Scheduler scheduler, ScheduleTriggerHandler triggerHandler) {
        this.scheduler = scheduler;
        this.triggerHandler = triggerHandler;
    }

    /**
     * 继承接口定义。
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        scheduler.getContext().put(AbstractPeachQuartzJob.CONTEXT_TRIGGER_HANDLER, triggerHandler);
    }
}
