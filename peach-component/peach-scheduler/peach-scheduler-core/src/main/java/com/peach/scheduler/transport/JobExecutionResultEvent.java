package com.peach.scheduler.transport;

import com.peach.scheduler.model.ExecutionResultStatus;
import java.time.Instant;

/**
 * 执行结果相关说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class JobExecutionResultEvent {

    private String executionId;
    private ExecutionResultStatus status;
    private String resultCode;
    private String errorMessage;
    private String executorInstance;
    private Instant startedAt;
    private Instant finishedAt;

    /**
     * 创建相关对象。
     */
    public JobExecutionResultEvent() {
    }

    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getExecutionId() {
        return executionId;
    }
    /**
     * 设置相关数据。
     *
     * @param executionId 参数说明
     */
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public ExecutionResultStatus getStatus() {
        return status;
    }
    /**
     * 设置相关数据。
     *
     * @param status 参数说明
     */
    public void setStatus(ExecutionResultStatus status) {
        this.status = status;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getResultCode() {
        return resultCode;
    }
    /**
     * 设置相关数据。
     *
     * @param resultCode 参数说明
     */
    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    /**
     * 设置相关数据。
     *
     * @param errorMessage 参数说明
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getExecutorInstance() {
        return executorInstance;
    }
    /**
     * 设置相关数据。
     *
     * @param executorInstance 参数说明
     */
    public void setExecutorInstance(String executorInstance) {
        this.executorInstance = executorInstance;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public Instant getStartedAt() {
        return startedAt;
    }
    /**
     * 设置相关数据。
     *
     * @param startedAt 参数说明
     */
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public Instant getFinishedAt() {
        return finishedAt;
    }
    /**
     * 设置相关数据。
     *
     * @param finishedAt 参数说明
     */
    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
