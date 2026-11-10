package com.peach.redisson.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redisson 客户端配置。保持现有 peach.redis.* 与旧 RedisConfig 属性兼容。
 * 只负责 Redisson，不要求 Spring Data Redis / Jedis。
 */
@Data
@ConfigurationProperties(prefix = "peach.redis")
public class PeachRedissonProperties {
    private String mode = "standalone";
    private String host;
    private String password;
    private String username;
    private String sentinelMaster = "master";
    private int database = 0;
    private RedissonOptions redisson = new RedissonOptions();

    @Data
    public static class RedissonOptions {
        private boolean enabled = true;
        private int threads = 16;
        private int nettyThreads = 32;
        private int timeout = 3000;
        private int connectTimeout = 10000;
        private int retryAttempts = 3;
        private int retryInterval = 1500;
        private int idleConnectionTimeout = 10000;
        private int pingTimeout = 1000;
        private int connectionPoolSize = 64;
        private int connectionMinimumIdleSize = 10;
        private int subscriptionConnectionPoolSize = 50;
        private int subscriptionConnectionMinimumIdleSize = 1;
        private int slaveConnectionMinimumIdleSize = 10;
        private int masterConnectionMinimumIdleSize = 10;
        private int slaveConnectionPoolSize = 64;
        private int masterConnectionPoolSize = 64;
        private int scanInterval = 1000;
        private int subscriptionsPerConnection = 5;
        private boolean sslEnable = false;
        private long lockWatchdogTimeout = 30000L;
    }
}
