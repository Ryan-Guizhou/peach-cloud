package com.peach.scheduler.mq;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduled.common.SchedulerConstants;
import com.peach.scheduler.service.ISchedulerExecutionService;
import com.peach.scheduler.transport.JobExecutionResultEvent;
import org.springframework.stereotype.Component;

/**
 * 调度模块相关说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Component
@MqConsumer(
        topic = SchedulerConstants.RESULT_TOPIC,
        tag = SchedulerConstants.RESULT_TAG,
        consumerGroup = SchedulerConstants.RESULT_CONSUMER_GROUP,
        idempotent = true)
@Indexed
public class SchedulerExecutionResultConsumer implements MqMessageHandler<JobExecutionResultEvent> {
    private final ISchedulerExecutionService executionService;

    /**
     * 创建相关对象。
     *
     * @param executionService 参数说明
     */
    public SchedulerExecutionResultConsumer(ISchedulerExecutionService executionService) {
        this.executionService = executionService;
    }

    /**
     * 继承接口定义。
     */
    @Override
    public void handle(JobExecutionResultEvent message, MqConsumeContext context) {
        executionService.processResult(message);
    }
}
