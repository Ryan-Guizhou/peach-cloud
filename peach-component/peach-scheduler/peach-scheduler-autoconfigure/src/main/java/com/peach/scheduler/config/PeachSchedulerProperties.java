package com.peach.scheduler.config;

import org.springframework.stereotype.Indexed;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Peach调度配置属性。
 * <p>该配置只描述业务应用接入 Scheduler Executor Runtime 所需的通用参数，不包含中央控制面的
 * Quartz、JDBC 或任务定义管理配置。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@ConfigurationProperties(prefix = "peach.scheduler")
@Indexed
public class PeachSchedulerProperties {

    /**
     * 是否启用 Scheduler 执行器自动配置。
     */
    private boolean enabled = true;

    /**
     * 业务执行器配置。
     */
    private final Executor executor = new Executor();

    /**
     * 创建默认 Scheduler 配置。
     */
    public PeachSchedulerProperties() {
        // Intentionally empty.
    }

    /**
     * 判断是否启用 Scheduler 执行器自动配置。
     *
     * @return 启用时返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用 Scheduler 执行器自动配置。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取业务执行器配置。
     *
     * @return 业务执行器配置
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * Executor。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2025/12/29 17:42
     */
    public static class Executor {

        /**
         * 当前业务应用名称，应与注册到 Scheduler 控制面的 applicationName 一致。
         */
        private String applicationName;

        /**
         * 当前执行器实例标识；为空时由运行时生成稳定的本实例标识。
         */
        private String instanceId;

        /**
         * 任务未显式指定超时时间时的默认超时时间，单位毫秒。
         */
        private long defaultTimeoutMs = 1800000L;

        /**
         * 持久化和回传错误摘要的最大字符数。
         */
        private int maxErrorMessageLength = 1000;

        /**
         * 创建默认业务执行器配置。
         */
        public Executor() {
            // Intentionally empty.
        }

        /**
         * 获取当前业务应用名称。
         *
         * @return 当前业务应用名称
         */
        public String getApplicationName() {
            return applicationName;
        }

        /**
         * 设置当前业务应用名称。
         *
         * @param applicationName 当前业务应用名称
         */
        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        /**
         * 获取当前执行器实例标识。
         *
         * @return 当前执行器实例标识
         */
        public String getInstanceId() {
            return instanceId;
        }

        /**
         * 设置当前执行器实例标识。
         *
         * @param instanceId 当前执行器实例标识
         */
        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }

        /**
         * 获取默认任务超时时间。
         *
         * @return 默认任务超时时间，单位毫秒
         */
        public long getDefaultTimeoutMs() {
            return defaultTimeoutMs;
        }

        /**
         * 设置默认任务超时时间。
         *
         * @param defaultTimeoutMs 默认任务超时时间，单位毫秒
         */
        public void setDefaultTimeoutMs(long defaultTimeoutMs) {
            this.defaultTimeoutMs = defaultTimeoutMs;
        }

        /**
         * 获取错误摘要最大字符数。
         *
         * @return 错误摘要最大字符数
         */
        public int getMaxErrorMessageLength() {
            return maxErrorMessageLength;
        }

        /**
         * 设置错误摘要最大字符数。
         *
         * @param maxErrorMessageLength 错误摘要最大字符数
         */
        public void setMaxErrorMessageLength(int maxErrorMessageLength) {
            this.maxErrorMessageLength = maxErrorMessageLength;
        }
    }
}
