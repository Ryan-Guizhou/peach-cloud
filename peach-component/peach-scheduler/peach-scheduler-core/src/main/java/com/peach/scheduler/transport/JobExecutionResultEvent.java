package com.peach.scheduler.transport;

import com.peach.scheduler.model.ExecutionResultStatus;
import java.time.Instant;

/**
 * 任务执行结果事件。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public record JobExecutionResultEvent(
        String executionId,
        ExecutionResultStatus status,
        String errorMessage,
        String resultCode,
        String executorInstance,
        Instant startedAt,
        Instant finishedAt) {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    public static final class Builder {

        private String executionId;
        private ExecutionResultStatus status;
        private String errorMessage;
        private String resultCode;
        private String executorInstance;
        private Instant startedAt;
        private Instant finishedAt;

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder status(ExecutionResultStatus status) {
            this.status = status;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder resultCode(String resultCode) {
            this.resultCode = resultCode;
            return this;
        }

        public Builder executorInstance(String executorInstance) {
            this.executorInstance = executorInstance;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder finishedAt(Instant finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public JobExecutionResultEvent build() {
            return new JobExecutionResultEvent(
                    executionId, status, errorMessage, resultCode, executorInstance, startedAt, finishedAt);
        }
    }
}
