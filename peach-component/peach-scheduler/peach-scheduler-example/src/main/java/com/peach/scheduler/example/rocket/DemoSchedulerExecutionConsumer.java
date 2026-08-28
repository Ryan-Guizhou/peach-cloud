package com.peach.scheduler.example.rocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.transport.JobExecutionCommand;
import org.springframework.stereotype.Component;

/**
 * 示例调度执行消费者。
 *
 * <p>调度模块说明。
 * 调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Slf4j
@Indexed
@Component
@MqConsumer(topic = "scheduler-execute-peach-scheduler-example",
        tag = "execute", consumerGroup = "peach-scheduler-example-executor", idempotent = true)
public class DemoSchedulerExecutionConsumer implements MqMessageHandler<JobExecutionCommand> {

    private final PeachJobExecutor executor;
    /**
     * 创建实例。
     *
     * @param executor executor。
     */
    public DemoSchedulerExecutionConsumer(PeachJobExecutor executor) {
        this.executor = executor;
    }
    /**
     * 接口实现。
     */
    @Override
    public void handle(JobExecutionCommand message, MqConsumeContext context) {

        executor.execute(message);
    }
}
