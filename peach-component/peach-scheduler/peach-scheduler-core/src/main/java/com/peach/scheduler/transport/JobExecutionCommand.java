package com.peach.scheduler.transport;

/**
 * 调度引擎无关的数据模型。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class JobExecutionCommand {

    private String executionId;
    private String jobCode;
    private String applicationName;
    private String handlerName;
    private String parameters;
    private long timeoutMs;
    private int attempt;
    private String traceId;

    /**
     * 创建相关对象。
     */
    public JobExecutionCommand() {
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
    public String getJobCode() {
        return jobCode;
    }
    /**
     * 设置相关数据。
     *
     * @param jobCode 参数说明
     */
    public void setJobCode(String jobCode) {
        this.jobCode = jobCode;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getApplicationName() {
        return applicationName;
    }
    /**
     * 设置相关数据。
     *
     * @param applicationName 参数说明
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getHandlerName() {
        return handlerName;
    }
    /**
     * 设置相关数据。
     *
     * @param handlerName 参数说明
     */
    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getParameters() {
        return parameters;
    }
    /**
     * 设置相关数据。
     *
     * @param parameters 参数说明
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }
    /**
     * 设置相关数据。
     *
     * @param timeoutMs 参数说明
     */
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public int getAttempt() {
        return attempt;
    }
    /**
     * 设置相关数据。
     *
     * @param attempt 参数说明
     */
    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }
    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getTraceId() {
        return traceId;
    }
    /**
     * 设置相关数据。
     *
     * @param traceId 参数说明
     */
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
