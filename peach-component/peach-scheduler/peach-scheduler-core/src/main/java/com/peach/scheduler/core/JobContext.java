package com.peach.scheduler.core;

/**
 * 不可变调度上下文。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public final class JobContext {

    private final String executionId;
    private final String jobCode;
    private final String applicationName;
    private final String parameters;
    private final int attempt;
    private final String traceId;

    /**
     * 创建相关对象。
     *
     * @param executionId 参数说明
     * @param jobCode 参数说明
     * @param applicationName 参数说明
     * @param parameters 参数说明
     * @param attempt 参数说明
     * @param traceId 参数说明
     */
    public JobContext(String executionId, String jobCode, String applicationName,
                      String parameters, int attempt, String traceId) {
        this.executionId = executionId;
        this.jobCode = jobCode;
        this.applicationName = applicationName;
        this.parameters = parameters;
        this.attempt = attempt;
        this.traceId = traceId;
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
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getJobCode() {
        return jobCode;
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
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getParameters() {
        return parameters;
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
     * 获取相关数据。
     *
     * @return 返回结果
     */
    public String getTraceId() {
        return traceId;
    }
}
