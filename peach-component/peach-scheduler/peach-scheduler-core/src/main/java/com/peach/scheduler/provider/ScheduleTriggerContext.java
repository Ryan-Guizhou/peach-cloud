package com.peach.scheduler.provider;

import java.time.Instant;

/**
 * 调度上下文说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public class ScheduleTriggerContext {
    private String jobCode;
    private Instant scheduledTime;
    private String parameters;
    private String providerId;

    /**
     * 创建相关对象。
     */
    public ScheduleTriggerContext() {
        // Intentionally empty.
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
    public Instant getScheduledTime() {
        return scheduledTime;
    }
    /**
     * 设置相关数据。
     *
     * @param scheduledTime 参数说明
     */
    public void setScheduledTime(Instant scheduledTime) {
        this.scheduledTime = scheduledTime;
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
    public String getProviderId() {
        return providerId;
    }
    /**
     * 设置相关数据。
     *
     * @param providerId 参数说明
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
}
