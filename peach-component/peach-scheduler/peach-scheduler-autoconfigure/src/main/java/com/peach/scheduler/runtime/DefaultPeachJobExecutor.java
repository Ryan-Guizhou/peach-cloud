package com.peach.scheduler.runtime;

import org.springframework.stereotype.Indexed;

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
import com.peach.threadpool.core.PoolType;
import com.peach.threadpool.manager.ThreadPoolManager;
import java.net.InetAddress;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认实现。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class DefaultPeachJobExecutor implements PeachJobExecutor {
    private static final Logger log = LoggerFactory.getLogger(DefaultPeachJobExecutor.class);
    private static final Pattern SENSITIVE_VALUE_PATTERN = Pattern.compile(
            "(?i)(password|passwd|token|secret|access[_-]?key|secret[_-]?key|credential)(\\s*[:=]\\s*)[^,;\\s}\\]]+");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)Bearer\\s+[^,;\\s]+");
    private final JobRegistry registry;
    private final ThreadPoolManager threadPoolManager;
    private final ExecutionLeaseClient leaseClient;
    private final ExecutionResultReporter resultReporter;
    private final PeachSchedulerProperties properties;

    /**
     * 创建相关对象。
     *
     * @param registry 参数说明
     * @param threadPoolManager 参数说明
     * @param leaseClient 参数说明
     * @param resultReporter 参数说明
     * @param properties 参数说明
     */
    public DefaultPeachJobExecutor(JobRegistry registry, ThreadPoolManager threadPoolManager,
                                   ExecutionLeaseClient leaseClient, ExecutionResultReporter resultReporter,
                                   PeachSchedulerProperties properties) {
        this.registry = registry;
        this.threadPoolManager = threadPoolManager;
        this.leaseClient = leaseClient;
        this.resultReporter = resultReporter;
        this.properties = properties;
    }

    /**
     * 继承接口定义。
     */
    @Override
    public void execute(JobExecutionCommand command) {
        validate(command);
        String instanceId = resolveInstanceId();
        if (!leaseClient.claim(command.getExecutionId(), instanceId)) {
            log.info("Scheduler execution claim rejected, executionId={}, jobCode={}, executorInstance={}",
                    command.getExecutionId(), command.getJobCode(), instanceId);
            return;
        }
        JobHandler handler = registry.getRequired(command.getHandlerName());
        Instant startedAt = Instant.now();
        log.info("Scheduler execution started, executionId={}, jobCode={}, handlerName={}, attempt={}, executorInstance={}",
                command.getExecutionId(), command.getJobCode(), command.getHandlerName(), command.getAttempt(), instanceId);
        Future<JobResult> future = threadPoolManager.submit(PoolType.SCHEDULED, () -> handler.execute(new JobContext(
                command.getExecutionId(), command.getJobCode(), command.getApplicationName(), command.getParameters(),
                Math.max(1, command.getAttempt()), command.getTraceId())));
        JobExecutionResultEvent event = new JobExecutionResultEvent();
        event.setExecutionId(command.getExecutionId());
        event.setExecutorInstance(instanceId);
        event.setStartedAt(startedAt);
        try {
            JobResult result = waitFor(future, command.getTimeoutMs());
            if (result == null || !result.isSuccess()) {
                event.setStatus(ExecutionResultStatus.FAILED);
                event.setResultCode(result == null ? "NULL_RESULT" : result.getCode());
                event.setErrorMessage(sanitize(result == null ? "Handler returned null result" : result.getMessage()));
            } else {
                event.setStatus(ExecutionResultStatus.SUCCEEDED);
                event.setResultCode(result.getCode());
            }
        } catch (TimeoutException ex) {
            future.cancel(true);
            event.setStatus(ExecutionResultStatus.TIMED_OUT);
            event.setResultCode("TIMEOUT");
            event.setErrorMessage("Execution exceeded configured timeout");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            event.setStatus(ExecutionResultStatus.FAILED);
            event.setResultCode("INTERRUPTED");
            event.setErrorMessage("Executor thread was interrupted");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            event.setStatus(ExecutionResultStatus.FAILED);
            event.setResultCode(cause.getClass().getSimpleName());
            event.setErrorMessage(sanitize(cause.getMessage()));
            log.error("Scheduler execution failed, executionId={}, jobCode={}, errorType={}",
                    command.getExecutionId(), command.getJobCode(), cause.getClass().getName());
        }
        event.setFinishedAt(Instant.now());
        resultReporter.report(event);
        log.info("Scheduler execution finished, executionId={}, jobCode={}, status={}, executorInstance={}",
                command.getExecutionId(), command.getJobCode(), event.getStatus(), instanceId);
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
        if (command == null || blank(command.getExecutionId()) || blank(command.getJobCode())
                || blank(command.getApplicationName()) || blank(command.getHandlerName())) {
            throw new IllegalArgumentException("Scheduler execution command is incomplete");
        }
        String localApp = properties.getExecutor().getApplicationName();
        if (!blank(localApp) && !localApp.equals(command.getApplicationName())) {
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

    private String sanitize(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.replace('\r', ' ').replace('\n', ' ');
        normalized = SENSITIVE_VALUE_PATTERN.matcher(normalized).replaceAll("$1$2***");
        normalized = BEARER_PATTERN.matcher(normalized).replaceAll("Bearer ***");
        int max = Math.max(64, properties.getExecutor().getMaxErrorMessageLength());
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
