package com.peach.scheduler.service;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.outbox.MqOutboxPublisher;
import com.peach.scheduled.common.SchedulerConstants;
import com.peach.scheduler.dispatch.JobDispatcher;
import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * RocketJobDispatcher相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class RocketJobDispatcher implements JobDispatcher {
    private final MqOutboxPublisher outboxPublisher;

    /**
     * 创建实例。
     *
     * @param outboxPublisher outbox Publisher。
     */
    public RocketJobDispatcher(MqOutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    /**
     * 接口实现。
     */
    @Override
    public void dispatch(JobExecutionCommand command) {
        MqSendOptions options = MqSendOptions.builder()
                .topic(SchedulerConstants.executionTopic(command.applicationName()))
                .tag("execute")
                .key(command.executionId())
                .build();
        outboxPublisher.publish(command, options);
    }
}
