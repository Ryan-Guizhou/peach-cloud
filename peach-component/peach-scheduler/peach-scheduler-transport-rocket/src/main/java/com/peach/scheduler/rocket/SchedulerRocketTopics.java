package com.peach.scheduler.rocket;

/**
 * SchedulerRocketTopics相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public final class SchedulerRocketTopics {
    /**
     * 执行结果相关说明。
     */
    public static final String EXECUTION_RESULT_TOPIC = "scheduler-execution-result";
    /**
     * 执行结果 Tag。
     */
    public static final String EXECUTION_RESULT_TAG = "result";
    private SchedulerRocketTopics() { }
    /**
     * 构建相关数据。
     * @param applicationName application Name。
     * @return 执行结果。
     */
    public static String executionTopic(String applicationName) {
        return "scheduler-execute-" + applicationName;
    }
}
