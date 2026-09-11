package com.peach.scheduler.quickstart.rocket;

import com.peach.rocket.annotation.MqConsumer;
import com.peach.rocket.core.MqConsumeContext;
import com.peach.rocket.core.MqMessageHandler;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.transport.JobExecutionCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 生产接线样例：消费控制面执行命令并交给 {@link PeachJobExecutor}。
 *
 * <p>本地 Quickstart 默认关闭 RocketMQ，本 Bean 不会装配。需要联调真实 MQ 时再打开
 * {@code peach.rocket.enabled=true}，且 Topic 必须是 {@code scheduler-execute-{app}}。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@MqConsumer(topic = "scheduler-execute-peach-scheduler-quickstart",
        tag = "execute", consumerGroup = "peach-scheduler-quickstart-executor", idempotent = true)
@ConditionalOnProperty(prefix = "peach.rocket", name = "enabled", havingValue = "true")
public class DemoSchedulerExecutionConsumer implements MqMessageHandler<JobExecutionCommand> {

    private final PeachJobExecutor executor;

    /**
     * 接收执行命令并委托给调度任务执行器；重复消息仍须先过 Claim。
     */
    @Override
    public void handle(JobExecutionCommand message, MqConsumeContext context) {
        log.info("demo consumer received command, executionId={}, jobCode={}",
                message.executionId(), message.jobCode());
        executor.execute(message);
    }
}
