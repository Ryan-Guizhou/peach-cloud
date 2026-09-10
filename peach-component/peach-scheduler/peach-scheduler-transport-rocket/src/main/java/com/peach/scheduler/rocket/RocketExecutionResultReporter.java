package com.peach.scheduler.rocket;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.core.MqSendOptions;
import com.peach.rocket.outbox.MqOutboxPublisher;
import com.peach.scheduler.transport.ExecutionResultReporter;
import com.peach.scheduler.transport.JobExecutionResultEvent;

/**
 * 基于 RocketMQ outbox 上报调度执行结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class RocketExecutionResultReporter implements ExecutionResultReporter {
    private final MqOutboxPublisher outboxPublisher;

    /**
     * 创建执行结果上报器。
     *
     * @param outboxPublisher RocketMQ outbox 发布器。
     */
    public RocketExecutionResultReporter(MqOutboxPublisher outboxPublisher) {
        this.outboxPublisher = outboxPublisher;
    }

    /**
     * 将执行结果事件发布到调度结果 topic。
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
