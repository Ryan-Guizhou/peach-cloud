package com.peach.scheduler.rocket;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.outbox.MqOutboxPublisher;
import com.peach.scheduler.transport.ExecutionResultReporter;
import com.peach.scheduler.transport.JobExecutionResultEvent;

/**
 * RocketExecutionResultReporter相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class RocketExecutionResultReporter implements ExecutionResultReporter {
    private final MqOutboxPublisher outboxPublisher;

    /**
     * 创建实例。
     *
     * @param outboxPublisher outbox Publisher。
     */
    public RocketExecutionResultReporter(MqOutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    /**
     * 接口实现。
     */
    @Override
    public void report(JobExecutionResultEvent event) {
        MqSendOptions options = MqSendOptions.builder()
                .topic(SchedulerRocketTopics.EXECUTION_RESULT_TOPIC)
                .tag(SchedulerRocketTopics.EXECUTION_RESULT_TAG)
                .key(event.executionId())
                .build();
        outboxPublisher.publish(event, options);
    }
}
