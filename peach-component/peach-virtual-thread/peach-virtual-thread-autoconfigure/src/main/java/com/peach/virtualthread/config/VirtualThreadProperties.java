package com.peach.virtualthread.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Peach 虚拟线程 Starter 配置。
 *
 * <p>配置前缀为 {@code peach.virtual-thread}。业务分组在应用启动期固定，Starter 不负责
 * 运行期动态扩缩容或配置中心策略变更。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:30
 */
@ConfigurationProperties(prefix = "peach.virtual-thread")
public class VirtualThreadProperties {

    /**
     * 是否启用虚拟线程 Starter 自动装配。
     */
    private boolean enabled = true;

    /**
     * 所有业务组虚拟线程名称的统一前缀。
     */
    private String threadNamePrefix = "peach-vt-";

    /**
     * Spring 容器关闭时等待所有业务组自然终止的最长时间。
     */
    private Duration shutdownAwait = Duration.ofSeconds(30);

    /**
     * 静态业务分组配置。Map key 即业务组名称。
     */
    private Map<String, Group> groups = new LinkedHashMap<>();

    /**
     * 是否启用 Starter 自动装配。
     *
     * @return true 表示启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置 Starter 自动装配开关。
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取虚拟线程名称统一前缀。
     *
     * @return 线程名称前缀
     */
    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    /**
     * 设置虚拟线程名称统一前缀。
     *
     * @param threadNamePrefix 线程名称前缀
     */
    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    /**
     * 获取 Spring 关闭阶段共享的优雅等待时间。
     *
     * @return 优雅关闭等待时间
     */
    public Duration getShutdownAwait() {
        return shutdownAwait;
    }

    /**
     * 设置 Spring 关闭阶段共享的优雅等待时间。
     *
     * @param shutdownAwait 优雅关闭等待时间
     */
    public void setShutdownAwait(Duration shutdownAwait) {
        this.shutdownAwait = shutdownAwait;
    }

    /**
     * 获取静态业务组配置。
     *
     * @return 业务组配置 Map，key 为业务组名称
     */
    public Map<String, Group> getGroups() {
        return groups;
    }

    /**
     * 设置静态业务组配置。
     *
     * @param groups 业务组配置；null 按空 Map 处理
     */
    public void setGroups(Map<String, Group> groups) {
        this.groups = groups == null ? new LinkedHashMap<>() : new LinkedHashMap<>(groups);
    }

    /**
     * 执行启动期配置校验。
     *
     * <p>校验只处理 Starter 自身能够确定的静态边界，不探测数据库连接池、HTTP 客户端或
     * 其他下游资源容量。</p>
     *
     * @throws IllegalStateException 配置不合法时抛出
     */
    public void validateForStartup() {
        if (!enabled) {
            return;
        }
        if (threadNamePrefix == null || threadNamePrefix.isBlank()) {
            throw new IllegalStateException("Property 'peach.virtual-thread.thread-name-prefix' must not be blank");
        }
        if (shutdownAwait == null || shutdownAwait.isNegative()) {
            throw new IllegalStateException("Property 'peach.virtual-thread.shutdown-await' must not be negative");
        }
        ensureNanosRepresentable(shutdownAwait, "shutdownAwait");
        for (Map.Entry<String, Group> entry : groups.entrySet()) {
            String groupName = entry.getKey();
            Group group = entry.getValue();
            if (groupName == null || groupName.isBlank()) {
                throw new IllegalStateException("Virtual thread group name must not be blank");
            }
            if (group == null) {
                throw new IllegalStateException("Virtual thread group config must not be null: " + groupName);
            }
            group.validate(groupName);
        }
    }

    /**
     * 校验 Duration 能够安全转换为纳秒，避免执行期因 {@link Duration#toNanos()} 溢出而失败。
     *
     * @param duration 待校验时长
     * @param propertyName 配置字段名称
     * @throws IllegalStateException 超出纳秒可表示范围时抛出
     */
    private static void ensureNanosRepresentable(Duration duration, String propertyName) {
        try {
            duration.toNanos();
        } catch (ArithmeticException ex) {
            throw new IllegalStateException(propertyName + " exceeds supported duration range", ex);
        }
    }

    /**
     * 单个业务组的静态并发配置。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/9/9 14:30
     */
    public static class Group {

        /**
         * 同时真正执行的最大业务任务数。
         */
        private int maxConcurrency = 256;

        /**
         * 已准入但暂时等待执行许可的最大任务数。
         */
        private int maxPending = 512;

        /**
         * Admission 容量耗尽后的背压策略。
         */
        private BackpressurePolicy backpressure = BackpressurePolicy.REJECT;

        /**
         * BLOCK 策略在调用线程上等待 Admission 许可的最长时间。
         */
        private Duration acquireTimeout = Duration.ofMillis(50);

        /**
         * 获取最大执行并发数。
         *
         * @return 最大执行并发数
         */
        public int getMaxConcurrency() {
            return maxConcurrency;
        }

        /**
         * 设置最大执行并发数。
         *
         * @param maxConcurrency 最大执行并发数
         */
        public void setMaxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
        }

        /**
         * 获取最大 Pending 任务数。
         *
         * @return 最大 Pending 任务数
         */
        public int getMaxPending() {
            return maxPending;
        }

        /**
         * 设置最大 Pending 任务数。
         *
         * @param maxPending 最大 Pending 任务数
         */
        public void setMaxPending(int maxPending) {
            this.maxPending = maxPending;
        }

        /**
         * 获取容量耗尽后的背压策略。
         *
         * @return 背压策略
         */
        public BackpressurePolicy getBackpressure() {
            return backpressure;
        }

        /**
         * 设置容量耗尽后的背压策略。
         *
         * @param backpressure 背压策略
         */
        public void setBackpressure(BackpressurePolicy backpressure) {
            this.backpressure = backpressure;
        }

        /**
         * 获取 BLOCK 模式等待 Admission Permit 的最长时间。
         *
         * @return 最大等待时间
         */
        public Duration getAcquireTimeout() {
            return acquireTimeout;
        }

        /**
         * 设置 BLOCK 模式等待 Admission Permit 的最长时间。
         *
         * @param acquireTimeout 最大等待时间
         */
        public void setAcquireTimeout(Duration acquireTimeout) {
            this.acquireTimeout = acquireTimeout;
        }

        /**
         * 校验单个业务组配置。
         *
         * @param groupName 业务组名称
         * @throws IllegalStateException 配置不合法时抛出
         */
        public void validate(String groupName) {
            if (maxConcurrency <= 0) {
                throw new IllegalStateException("maxConcurrency must be greater than zero: " + groupName);
            }
            if (maxPending < 0) {
                throw new IllegalStateException("maxPending must not be negative: " + groupName);
            }
            if ((long) maxConcurrency + maxPending > Integer.MAX_VALUE) {
                throw new IllegalStateException(
                        "maxConcurrency + maxPending exceeds supported capacity: " + groupName);
            }
            if (backpressure == null) {
                throw new IllegalStateException("backpressure must not be null: " + groupName);
            }
            if (backpressure == BackpressurePolicy.BLOCK
                    && (acquireTimeout == null || acquireTimeout.isZero() || acquireTimeout.isNegative())) {
                throw new IllegalStateException("BLOCK backpressure requires positive acquireTimeout: " + groupName);
            }
            if (backpressure == BackpressurePolicy.BLOCK) {
                ensureNanosRepresentable(acquireTimeout, "acquireTimeout: " + groupName);
            }
        }
    }
}
