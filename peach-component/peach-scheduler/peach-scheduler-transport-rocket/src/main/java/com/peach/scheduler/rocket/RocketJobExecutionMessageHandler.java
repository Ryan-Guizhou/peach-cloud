package com.peach.scheduler.rocket;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * RocketMQ任务执行消息处理器。
 * <p>调度模块说明。
 * 调度模块说明。
 * 调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class RocketJobExecutionMessageHandler implements MqMessageHandler<JobExecutionCommand> {
    private final PeachJobExecutor executor;

    /**
     * 创建实例。
     *
     * @param executor executor。
     */
    public RocketJobExecutionMessageHandler(PeachJobExecutor executor) {
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
