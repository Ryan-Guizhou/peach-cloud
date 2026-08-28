package com.peach.scheduler.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduled.common.SchedulerConstants;
import com.peach.scheduler.service.ISchedulerExecutionService;
import com.peach.scheduler.transport.JobExecutionResultEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 调度执行结果消费者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Slf4j
@Indexed
@Component
@MqConsumer(
        topic = SchedulerConstants.RESULT_TOPIC,
        tag = SchedulerConstants.RESULT_TAG,
        consumerGroup = SchedulerConstants.RESULT_CONSUMER_GROUP,
        idempotent = true)
@RequiredArgsConstructor
public class SchedulerExecutionResultConsumer implements MqMessageHandler<JobExecutionResultEvent> {

    private final ISchedulerExecutionService executionService;

    /**
     * 接口实现。
     */
    @Override
    public void handle(JobExecutionResultEvent message, MqConsumeContext context) {
        executionService.processResult(message);
    }
}
