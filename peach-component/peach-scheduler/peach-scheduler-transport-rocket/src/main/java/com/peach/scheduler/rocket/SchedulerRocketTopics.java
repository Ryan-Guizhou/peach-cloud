package com.peach.scheduler.rocket;

/**
 * 调度 RocketMQ topic 与 tag 命名工具。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public final class SchedulerRocketTopics {
    /**
     * 调度执行结果 topic。
     */
    public static final String EXECUTION_RESULT_TOPIC = "scheduler-execution-result";
    /**
     * 执行结果 Tag。
     */
    public static final String EXECUTION_RESULT_TAG = "result";
    private SchedulerRocketTopics() { }
    /**
     * 根据应用名称构建执行命令 topic。
     *
     * @param applicationName 目标应用名称。
     * @return 执行命令 topic。
     */
    public static String executionTopic(String applicationName) {
        return "scheduler-execute-" + applicationName;
    }
}
