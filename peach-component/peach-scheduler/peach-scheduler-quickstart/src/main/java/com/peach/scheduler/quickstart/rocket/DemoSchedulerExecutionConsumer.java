package com.peach.scheduler.quickstart.rocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.transport.JobExecutionCommand;
import org.springframework.stereotype.Component;

/**
 * 调度 quickstart 的 RocketMQ 执行命令消费者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Slf4j
@Indexed
@Component
@MqConsumer(topic = "scheduler-execute-peach-scheduler-quickstart",
        tag = "execute", consumerGroup = "peach-scheduler-quickstart-executor", idempotent = true)
public class DemoSchedulerExecutionConsumer implements MqMessageHandler<JobExecutionCommand> {

    private final PeachJobExecutor executor;
    /**
     * 创建示例执行命令消费者。
     *
     * @param executor 调度任务执行器。
     */
    public DemoSchedulerExecutionConsumer(PeachJobExecutor executor) {
        this.executor = executor;
    }
    /**
     * 接收执行命令并委托给调度任务执行器。
     */
    @Override
    public void handle(JobExecutionCommand message, MqConsumeContext context) {

        executor.execute(message);
    }
}
