package com.peach.scheduler.config;

/**
 * 从配置中心读取的首次任务登记定义；不包含运行状态或任务身份。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 15:00
 */
public class JobRegistrationDefinition {
    private String scheduleType;
    private String cronExpression;
    private Long intervalSeconds;
    private String startAt;
    private String timeZone;
    private String misfirePolicy;
    private String concurrencyPolicy;
    private Long timeoutMs;
    private Integer maxAttempts;
    private Integer retryIntervalSeconds;
    private String parametersJson;

    /** @return scheduleType 对应的登记配置值 */
    public String getScheduleType() { return scheduleType; }

    /** @param scheduleType 登记配置值 */
    public void setScheduleType(String scheduleType) { this.scheduleType = scheduleType; }

    /** @return cronExpression 对应的登记配置值 */
    public String getCronExpression() { return cronExpression; }

    /** @param cronExpression 登记配置值 */
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    /** @return intervalSeconds 对应的登记配置值 */
    public Long getIntervalSeconds() { return intervalSeconds; }

    /** @param intervalSeconds 登记配置值 */
    public void setIntervalSeconds(Long intervalSeconds) { this.intervalSeconds = intervalSeconds; }

    /** @return startAt 对应的登记配置值 */
    public String getStartAt() { return startAt; }

    /** @param startAt 登记配置值 */
    public void setStartAt(String startAt) { this.startAt = startAt; }

    /** @return timeZone 对应的登记配置值 */
    public String getTimeZone() { return timeZone; }

    /** @param timeZone 登记配置值 */
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }

    /** @return misfirePolicy 对应的登记配置值 */
    public String getMisfirePolicy() { return misfirePolicy; }

    /** @param misfirePolicy 登记配置值 */
    public void setMisfirePolicy(String misfirePolicy) { this.misfirePolicy = misfirePolicy; }

    /** @return concurrencyPolicy 对应的登记配置值 */
    public String getConcurrencyPolicy() { return concurrencyPolicy; }

    /** @param concurrencyPolicy 登记配置值 */
    public void setConcurrencyPolicy(String concurrencyPolicy) { this.concurrencyPolicy = concurrencyPolicy; }

    /** @return timeoutMs 对应的登记配置值 */
    public Long getTimeoutMs() { return timeoutMs; }

    /** @param timeoutMs 登记配置值 */
    public void setTimeoutMs(Long timeoutMs) { this.timeoutMs = timeoutMs; }

    /** @return maxAttempts 对应的登记配置值 */
    public Integer getMaxAttempts() { return maxAttempts; }

    /** @param maxAttempts 登记配置值 */
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

    /** @return retryIntervalSeconds 对应的登记配置值 */
    public Integer getRetryIntervalSeconds() { return retryIntervalSeconds; }

    /** @param retryIntervalSeconds 登记配置值 */
    public void setRetryIntervalSeconds(Integer retryIntervalSeconds) { this.retryIntervalSeconds = retryIntervalSeconds; }

    /** @return parametersJson 对应的登记配置值 */
    public String getParametersJson() { return parametersJson; }

    /** @param parametersJson 登记配置值 */
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }
}
