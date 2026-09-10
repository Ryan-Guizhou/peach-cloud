package com.peach.scheduler.runtime;

import org.springframework.stereotype.Indexed;

import com.peach.common.util.desensitize.DesensitizeUtil;
import com.peach.scheduler.config.PeachSchedulerProperties;
import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobHandler;
import com.peach.scheduler.core.JobRegistry;
import com.peach.scheduler.core.JobResult;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.model.ExecutionResultStatus;
import com.peach.scheduler.transport.ExecutionLeaseClient;
import com.peach.scheduler.transport.ExecutionResultReporter;
import com.peach.scheduler.transport.JobExecutionCommand;
import com.peach.scheduler.transport.JobExecutionResultEvent;
import com.peach.virtualthread.registry.VirtualExecutorRegistry;
import java.net.InetAddress;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler 默认任务执行编排器。
 *
 * <p>执行器先使用 {@link ExecutionLeaseClient} 抢占控制面下发的 execution 租约，成功后
 * 将业务 Handler 提交到 {@code scheduler} 虚拟线程业务组执行，并把成功、失败、超时或中断
 * 结果通过 {@link ExecutionResultReporter} 回传。该类不负责 Quartz 触发、任务定义管理或
 * Handler 内部业务幂等。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class DefaultPeachJobExecutor implements PeachJobExecutor {
    private static final Logger log = LoggerFactory.getLogger(DefaultPeachJobExecutor.class);
    private static final String SCHEDULER_EXECUTOR_GROUP = "scheduler";

    private final JobRegistry registry;
    private final VirtualExecutorRegistry virtualExecutorRegistry;
    private final ExecutionLeaseClient leaseClient;
    private final ExecutionResultReporter resultReporter;
    private final PeachSchedulerProperties properties;

    /**
     * 创建默认任务执行编排器。
     *
     * @param registry Handler 注册表
     * @param virtualExecutorRegistry 虚拟线程执行器注册中心
     * @param leaseClient 执行租约客户端
     * @param resultReporter 执行结果上报器
     * @param properties Scheduler 配置属性
     */
    public DefaultPeachJobExecutor(JobRegistry registry, VirtualExecutorRegistry virtualExecutorRegistry,
                                   ExecutionLeaseClient leaseClient, ExecutionResultReporter resultReporter,
                                   PeachSchedulerProperties properties) {
        this.registry = registry;
        this.virtualExecutorRegistry = virtualExecutorRegistry;
        this.leaseClient = leaseClient;
        this.resultReporter = resultReporter;
        this.properties = properties;
        this.virtualExecutorRegistry.get(SCHEDULER_EXECUTOR_GROUP);
    }

    /**
     * 执行控制面下发的单次任务命令。
     *
     * <p>该方法会同步等待 Handler 结果或超时，超时后会取消受管 Future。业务副作用是否能被
     * 取消取决于 Handler 和底层 IO 对中断的响应能力。</p>
     *
     * @param command 任务执行命令
     */
    @Override
    public void execute(JobExecutionCommand command) {
        validate(command);
        String instanceId = resolveInstanceId();
        if (!leaseClient.claim(command.executionId(), instanceId)) {
            log.info("Scheduler execution claim rejected, executionId={}, jobCode={}, executorInstance={}",
                    command.executionId(), command.jobCode(), instanceId);
            return;
        }
        JobHandler handler = registry.getRequired(command.handlerName());
        Instant startedAt = Instant.now();
        log.info("Scheduler execution started, executionId={}, jobCode={}, handlerName={}, attempt={}, executorInstance={}",
                command.executionId(), command.jobCode(), command.handlerName(), command.attempt(), instanceId);
        Future<JobResult> future = virtualExecutorRegistry.submit(SCHEDULER_EXECUTOR_GROUP, () -> handler.execute(new JobContext(
                command.executionId(), command.jobCode(), command.applicationName(), command.parameters(),
                Math.max(1, command.attempt()), command.traceId())));
        JobExecutionResultEvent.Builder eventBuilder = JobExecutionResultEvent.builder()
                .executionId(command.executionId())
                .executorInstance(instanceId)
                .startedAt(startedAt);
        try {
            JobResult result = waitFor(future, command.timeoutMs());
            if (result == null || !result.isSuccess()) {
                eventBuilder.status(ExecutionResultStatus.FAILED)
                        .resultCode(result == null ? "NULL_RESULT" : result.getCode())
                        .errorMessage(DesensitizeUtil.sanitizeErrorMessage(
                                result == null ? "Handler returned null result" : result.getMessage(),
                                Math.max(64, properties.getExecutor().getMaxErrorMessageLength())));
            } else {
                eventBuilder.status(ExecutionResultStatus.SUCCEEDED)
                        .resultCode(result.getCode());
            }
        } catch (TimeoutException ex) {
            future.cancel(true);
            eventBuilder.status(ExecutionResultStatus.TIMED_OUT)
                    .resultCode("TIMEOUT")
                    .errorMessage("Execution exceeded configured timeout");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            eventBuilder.status(ExecutionResultStatus.FAILED)
                    .resultCode("INTERRUPTED")
                    .errorMessage("Executor thread was interrupted");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            eventBuilder.status(ExecutionResultStatus.FAILED)
                    .resultCode(cause.getClass().getSimpleName())
                    .errorMessage(DesensitizeUtil.sanitizeErrorMessage(cause.getMessage(),
                            Math.max(64, properties.getExecutor().getMaxErrorMessageLength())));
            log.error("Scheduler execution failed, executionId={}, jobCode={}, errorType={}",
                    command.executionId(), command.jobCode(), cause.getClass().getName());
        }
        JobExecutionResultEvent event = eventBuilder.finishedAt(Instant.now()).build();
        resultReporter.report(event);
        log.info("Scheduler execution finished, executionId={}, jobCode={}, status={}, executorInstance={}",
                command.executionId(), command.jobCode(), event.status(), instanceId);
    }

    private JobResult waitFor(Future<JobResult> future, long commandTimeoutMs)
            throws InterruptedException, ExecutionException, TimeoutException {
        long timeoutMs = commandTimeoutMs > 0 ? commandTimeoutMs : properties.getExecutor().getDefaultTimeoutMs();
        if (timeoutMs <= 0) {
            return future.get();
        }
        return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void validate(JobExecutionCommand command) {
        if (command == null || blank(command.executionId()) || blank(command.jobCode())
                || blank(command.applicationName()) || blank(command.handlerName())) {
            throw new IllegalArgumentException("Scheduler execution command is incomplete");
        }
        String localApp = properties.getExecutor().getApplicationName();
        if (!blank(localApp) && !localApp.equals(command.applicationName())) {
            throw new IllegalArgumentException("Scheduler command target application does not match local application");
        }
    }

    private String resolveInstanceId() {
        if (!blank(properties.getExecutor().getInstanceId())) {
            return properties.getExecutor().getInstanceId();
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "unknown-instance";
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
