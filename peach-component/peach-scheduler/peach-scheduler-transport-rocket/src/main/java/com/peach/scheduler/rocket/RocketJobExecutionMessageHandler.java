package com.peach.scheduler.rocket;

import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.transport.JobExecutionCommand;

/**
 * 调度模块相关说明。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class RocketJobExecutionMessageHandler implements MqMessageHandler<JobExecutionCommand> {
    private final PeachJobExecutor executor;

    /**
     * 创建相关对象。
     *
     * @param executor 参数说明
     */
    public RocketJobExecutionMessageHandler(PeachJobExecutor executor) {
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
