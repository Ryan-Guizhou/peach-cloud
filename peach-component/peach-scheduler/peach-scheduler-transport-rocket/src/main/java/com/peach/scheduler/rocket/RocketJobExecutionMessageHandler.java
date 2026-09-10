package com.peach.scheduler.rocket;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * RocketMQ 执行命令消息处理器，负责把执行命令交给 Peach 调度执行器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class RocketJobExecutionMessageHandler implements MqMessageHandler<JobExecutionCommand> {
    private final PeachJobExecutor executor;

    /**
     * 创建执行命令消息处理器。
     *
     * @param executor 调度任务执行器。
     */
    public RocketJobExecutionMessageHandler(PeachJobExecutor executor) {
        this.executor = executor;
    }

    /**
     * 接收 RocketMQ 执行命令并触发本地任务执行。
     */
    @Override
    public void handle(JobExecutionCommand message, MqConsumeContext context) {
        executor.execute(message);
    }
}
