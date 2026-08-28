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
 * DefaultPeachJobExecutor相关类。
 * <p>调度模块说明。
 * 调度模块说明。
 * 调度模块说明。</p>
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
     * 接口实现。
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
        Future<JobResult> future = threadPoolManager.submit(PoolType.SCHEDULED, () -> handler.execute(new JobContext(
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
                        .errorMessage(sanitize(result == null ? "Handler returned null result" : result.getMessage()));
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
                    .errorMessage(sanitize(cause.getMessage()));
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
