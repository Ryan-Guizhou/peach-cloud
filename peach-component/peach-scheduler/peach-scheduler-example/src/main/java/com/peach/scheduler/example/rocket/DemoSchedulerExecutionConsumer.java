package com.peach.scheduler.example.rocket;

import org.springframework.stereotype.Indexed;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.transport.JobExecutionCommand;
import org.springframework.stereotype.Component;

/**
 * 调度模块相关说明。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Component
@MqConsumer(topic = "scheduler-execute-peach-scheduler-example",
        tag = "execute", consumerGroup = "peach-scheduler-example-executor", idempotent = true)
@Indexed
public class DemoSchedulerExecutionConsumer implements MqMessageHandler<JobExecutionCommand> {
    private final PeachJobExecutor executor;
    /**
     * 创建相关对象。
     *
     * @param executor 参数说明
     */
    public DemoSchedulerExecutionConsumer(PeachJobExecutor executor) {
        this.executor = executor;
    }
    /**
     * 继承接口定义。
     */
    @Override
    public void handle(JobExecutionCommand message, MqConsumeContext context) {
        executor.execute(message);
    }
}
