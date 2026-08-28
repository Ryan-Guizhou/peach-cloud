package com.peach.scheduler.quartz;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.provider.ScheduleTriggerHandler;
import com.peach.scheduler.provider.SchedulingProvider;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * PeachQuartz自动配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@AutoConfiguration
@EnableConfigurationProperties(PeachQuartzProperties.class)
@Indexed
public class PeachQuartzAutoConfiguration {

    /**
     * 注册相关能力。
     * @param scheduler scheduler。
     * @param triggerHandler trigger Handler。
     * @return 执行结果。
     */
    @Bean
    @ConditionalOnBean(ScheduleTriggerHandler.class)
    public QuartzTriggerHandlerRegistrar quartzTriggerHandlerRegistrar(Scheduler scheduler, ScheduleTriggerHandler triggerHandler) {
        return new QuartzTriggerHandlerRegistrar(scheduler, triggerHandler);
    }

    /**
     * 创建实例。
     * @param scheduler scheduler。
     * @param properties properties。
     * @return 执行结果。
     */
    @Bean
    @ConditionalOnMissingBean(name = "quartzSchedulingProvider")
    public SchedulingProvider quartzSchedulingProvider(Scheduler scheduler, PeachQuartzProperties properties) {
        return new QuartzSchedulingProvider(scheduler, properties);
    }
}
