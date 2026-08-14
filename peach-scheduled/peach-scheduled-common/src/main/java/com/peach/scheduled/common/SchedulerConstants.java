package com.peach.scheduled.common;
/**
 * 调度平台稳定常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public final class SchedulerConstants {
    /**
     * 执行结果 Topic。
     */
    public static final String RESULT_TOPIC = "scheduler-execution-result";
    /**
     * 执行结果 Tag。
     */
    public static final String RESULT_TAG = "result";
    /**
     * 执行结果消费者组。
     */
    public static final String RESULT_CONSUMER_GROUP = "peach-scheduler-result";
    private SchedulerConstants() { }
    /**
     * 构建相关数据。
     *
     * @param applicationName 参数说明
     * @return 返回结果
     */
    public static String executionTopic(String applicationName) {
        return "scheduler-execute-" + applicationName;
    }
}
