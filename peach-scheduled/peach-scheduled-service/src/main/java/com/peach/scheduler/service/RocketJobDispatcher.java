package com.peach.scheduler.service;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.outbox.MqOutboxPublisher;
import com.peach.scheduled.common.SchedulerConstants;
import com.peach.scheduler.dispatch.JobDispatcher;
import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * 分布式调度相关实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class RocketJobDispatcher implements JobDispatcher {
    private final MqOutboxPublisher outboxPublisher;

    /**
     * 创建相关对象。
     *
     * @param outboxPublisher 参数说明
     */
    public RocketJobDispatcher(MqOutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    /**
     * 继承接口定义。
     */
    @Override
    public void dispatch(JobExecutionCommand command) {
        MqSendOptions options = MqSendOptions.builder()
                .topic(SchedulerConstants.executionTopic(command.getApplicationName()))
                .tag("execute")
                .key(command.getExecutionId())
                .build();
        outboxPublisher.publish(command, options);
    }
}
