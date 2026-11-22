package com.peach.auth.launch.scheduler;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.transport.JobExecutionCommand;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * auth 服务测试执行命令入口，统一通过执行器完成 Claim 和结果上报。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 22:00
 */
@Indexed
@Component
@MqConsumer(topic = "scheduler-execute-peach-auth", tag = "execute",
        consumerGroup = "peach-auth-scheduler-demo-executor", idempotent = true)
@ConditionalOnProperty(name = "peach.scheduler.demo.enabled", havingValue = "true")
public class AuthSchedulerDemoConsumer implements MqMessageHandler<JobExecutionCommand> {
    private final PeachJobExecutor executor;

    /**
     * 创建命令消费者。
     * @param executor 受管任务执行器
     */
    public AuthSchedulerDemoConsumer(PeachJobExecutor executor) {
        this.executor = executor;
    }

    /**
     * 委托执行器处理命令，禁止绕过租约直接调用 Handler。
     * @param command 调度命令
     * @param context 消费上下文
     */
    @Override
    public void handle(JobExecutionCommand command, MqConsumeContext context) {
        executor.execute(command);
    }
}
