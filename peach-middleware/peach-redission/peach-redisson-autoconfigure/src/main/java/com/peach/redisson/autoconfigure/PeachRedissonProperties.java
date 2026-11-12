package com.peach.redisson.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redisson 客户端配置。
 *
 * <p>配置前缀为 {@code peach.redis}，保持与现有 {@code peach.redis.*} 及旧
 * {@code RedisConfig} 属性的兼容。本类只负责 Redisson 客户端本身的参数，
 * 不依赖 Spring Data Redis，也不要求引入 Jedis。</p>
 *
 * <p>模式约定：{@link #mode} 支持 {@code standalone}、{@code sentinel}、
 * {@code cluster}、{@code replicated} 等 Redisson 原生模式字符串，
 * 具体由自动配置类解析后决定构建哪种 Redisson 配置。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Data
@ConfigurationProperties(prefix = "peach.redis")
public class PeachRedissonProperties {

    /**
     * 连接模式。
     * <p>默认 {@code standalone}；可选 {@code sentinel}、{@code cluster}、
     * {@code replicated} 等，需与自动配置类的解析逻辑保持一致。</p>
     */
    private String mode = "standalone";

    /**
     * Redis 主机地址，standalone 模式下必填。
     */
    private String host;

    /**
     * Redis 访问密码，无密码时留空。
     */
    private String password;

    /**
     * Redis 访问用户名，Redis 6+ ACL 场景使用，普通密码认证时留空。
     */
    private String username;

    /**
     * Sentinel 模式下的 master 名称，默认 {@code master}。
     */
    private String sentinelMaster = "master";

    /**
     * Sentinel 自身的访问用户名，Sentinel 开启 ACL 时使用。
     */
    private String sentinelUsername;

    /**
     * Sentinel 自身的访问密码，Sentinel 开启密码认证时使用。
     */
    private String sentinelPassword;

    /**
     * 使用的 Redis 数据库编号，默认 {@code 0}。
     * <p>仅 standalone / sentinel 模式有效，cluster 模式固定为 0。</p>
     */
    private int database = 0;

    /**
     * Redisson 底层客户端参数，对应 {@code peach.redis.redisson.*}。
     */
    private RedissonOptions redisson = new RedissonOptions();

    /**
     * Redisson 客户端细粒度参数。
     *
     * <p>字段名与 Redisson 的 {@code Config} 及其子配置基本一一对应，
     * 单位除特别标注外均为毫秒。默认值面向一般业务场景，
     * 高并发或弱网环境需按实际情况调大连接池、超时和重试相关参数。</p>
     */
    @Data
    public static class RedissonOptions {

        /**
         * 是否启用 Redisson 自动配置，默认 {@code true}。
         * <p>设为 {@code false} 时自动配置类应跳过 Bean 注册。</p>
         */
        private boolean enabled = true;

        /**
         * Redisson 业务线程池大小，默认 {@code 16}。
         * <p>影响异步命令、分布式锁看门狗等任务的执行能力。</p>
         */
        private int threads = 16;

        /**
         * Netty IO 线程数，默认 {@code 32}。
         * <p>一般建议不小于 CPU 核数，过高会增加上下文切换开销。</p>
         */
        private int nettyThreads = 32;

        /**
         * 命令执行超时时间，单位毫秒，默认 {@code 3000}。
         */
        private int timeout = 3000;

        /**
         * 建立连接超时时间，单位毫秒，默认 {@code 10000}。
         */
        private int connectTimeout = 10000;

        /**
         * 命令失败重试次数，默认 {@code 3}。
         */
        private int retryAttempts = 3;

        /**
         * 重试间隔，单位毫秒，默认 {@code 1500}。
         */
        private int retryInterval = 1500;

        /**
         * 连接空闲超时时间，单位毫秒，默认 {@code 10000}。
         * <p>超过该时间未使用的连接会被回收。</p>
         */
        private int idleConnectionTimeout = 10000;

        /**
         * PING 命令超时时间，单位毫秒，默认 {@code 1000}。
         */
        private int pingTimeout = 1000;

        /**
         * 普通连接池最大连接数，默认 {@code 64}。
         */
        private int connectionPoolSize = 64;

        /**
         * 普通连接池最小空闲连接数，默认 {@code 10}。
         */
        private int connectionMinimumIdleSize = 10;

        /**
         * 订阅连接池最大连接数，默认 {@code 50}。
         * <p>Pub/Sub、分布式锁看门狗等依赖订阅连接。</p>
         */
        private int subscriptionConnectionPoolSize = 50;

        /**
         * 订阅连接池最小空闲连接数，默认 {@code 1}。
         */
        private int subscriptionConnectionMinimumIdleSize = 1;

        /**
         * 从节点最小空闲连接数，默认 {@code 10}。
         * <p>仅主从 / sentinel / cluster 模式有效。</p>
         */
        private int slaveConnectionMinimumIdleSize = 10;

        /**
         * 主节点最小空闲连接数，默认 {@code 10}。
         * <p>仅主从 / sentinel / cluster 模式有效。</p>
         */
        private int masterConnectionMinimumIdleSize = 10;

        /**
         * 从节点连接池最大连接数，默认 {@code 64}。
         * <p>仅主从 / sentinel / cluster 模式有效。</p>
         */
        private int slaveConnectionPoolSize = 64;

        /**
         * 主节点连接池最大连接数，默认 {@code 64}。
         * <p>仅主从 / sentinel / cluster 模式有效。</p>
         */
        private int masterConnectionPoolSize = 64;

        /**
         * 集群拓扑扫描间隔，单位毫秒，默认 {@code 1000}。
         * <p>仅 cluster 模式有效，用于发现节点变化。</p>
         */
        private int scanInterval = 1000;

        /**
         * 每个连接上允许的订阅数量，默认 {@code 5}。
         */
        private int subscriptionsPerConnection = 5;

        /**
         * 是否启用 SSL 连接，默认 {@code false}。
         */
        private boolean sslEnable = false;

        /**
         * 分布式锁看门狗超时时间，单位毫秒，默认 {@code 30000}。
         * <p>未显式指定 leaseTime 的锁会以此作为续期基准。</p>
         */
        private long lockWatchdogTimeout = 30000L;
    }
}
