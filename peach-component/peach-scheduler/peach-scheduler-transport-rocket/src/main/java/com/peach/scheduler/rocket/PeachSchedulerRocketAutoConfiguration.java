package com.peach.scheduler.rocket;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.outbox.MqOutboxPublisher;
import com.peach.scheduler.transport.ExecutionResultReporter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Peach调度RocketMQ自动配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@AutoConfiguration
@Indexed
public class PeachSchedulerRocketAutoConfiguration {

    /**
     * 创建实例。
     * @param outboxPublisher outbox Publisher。
     * @return 执行结果。
     */
    @Bean
    @ConditionalOnBean(MqOutboxPublisher.class)
    @ConditionalOnMissingBean(ExecutionResultReporter.class)
    public ExecutionResultReporter schedulerExecutionResultReporter(MqOutboxPublisher outboxPublisher) {
        return new RocketExecutionResultReporter(outboxPublisher);
    }
}
