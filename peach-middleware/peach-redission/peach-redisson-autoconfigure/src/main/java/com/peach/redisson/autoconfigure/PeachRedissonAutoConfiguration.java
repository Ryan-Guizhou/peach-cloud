package com.peach.redisson.autoconfigure;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

/**
 * Redisson-only 自动装配。
 *
 * <p>先于此类加载的旧 RedisConfig 若已注册 RedissonClient，则复用旧 Bean；
 * 后续移除旧混合装配时本配置直接提供客户端。自定义 RedissonClient 优先。</p>
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@AutoConfiguration
@AutoConfigureAfter(name = "com.peach.redis.common.RedisConfig")
@ConditionalOnClass(RedissonClient.class)
@ConditionalOnProperty(prefix = "peach.redis.redisson", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachRedissonProperties.class)
public class PeachRedissonAutoConfiguration {

    @Bean(name = "redissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(PeachRedissonProperties props) {
        if (props.getHost() == null || props.getHost().isBlank()) {
            throw new IllegalArgumentException("peach.redis.host is required for Redisson");
        }
        PeachRedissonProperties.RedissonOptions p = props.getRedisson();
        if (p.getConnectionPoolSize() < p.getConnectionMinimumIdleSize()
                || p.getMasterConnectionPoolSize() < p.getMasterConnectionMinimumIdleSize()
                || p.getSlaveConnectionPoolSize() < p.getSlaveConnectionMinimumIdleSize()) {
            throw new IllegalArgumentException("Redisson pool size must be >= minimum idle size");
        }

        Config config = new Config();
        config.setThreads(p.getThreads());
        config.setNettyThreads(p.getNettyThreads());
        config.setLockWatchdogTimeout(p.getLockWatchdogTimeout());
        config.setCodec(new JsonJacksonCodec());

        String mode = props.getMode();
        if (mode == null) {
            throw new IllegalArgumentException("peach.redis.mode is required for Redisson");
        }
        String scheme = p.isSslEnable() ? "rediss://" : "redis://";
        String[] addresses = Arrays.stream(props.getHost().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> normalizedAddress(scheme, s))
                .toArray(String[]::new);
        if (addresses.length == 0) {
            throw new IllegalArgumentException("No Redis nodes configured for Redisson");
        }

        switch (mode) {
            case "standalone" -> configureStandalone(config, props, p, addresses);
            case "sentinel" -> configureSentinel(config, props, p, addresses);
            case "cluster" -> configureCluster(config, props, p, addresses);
            default -> throw new IllegalArgumentException("Unsupported peach.redis.mode: " + mode);
        }
        return Redisson.create(config);
    }

    private static String normalizedAddress(String scheme, String hostPort) {
        int separator = hostPort.lastIndexOf(':');
        if (separator <= 0 || separator == hostPort.length() - 1) {
            throw new IllegalArgumentException("Redis node must be host:port: " + hostPort);
        }
        int port;
        try {
            port = Integer.parseInt(hostPort.substring(separator + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Redis port in: " + hostPort, e);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid Redis port in: " + hostPort);
        }
        return scheme + hostPort;
    }

    private static void configureStandalone(Config config, PeachRedissonProperties props,
                                            PeachRedissonProperties.RedissonOptions p, String[] addresses) {
        if (addresses.length != 1) {
            throw new IllegalArgumentException("Standalone Redis requires one host:port");
        }
        SingleServerConfig server = config.useSingleServer()
                .setAddress(addresses[0])
                .setDatabase(props.getDatabase())
                .setConnectionPoolSize(p.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(p.getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(p.getSubscriptionConnectionPoolSize())
                .setSubscriptionConnectionMinimumIdleSize(p.getSubscriptionConnectionMinimumIdleSize())
                .setTimeout(p.getTimeout())
                .setConnectTimeout(p.getConnectTimeout())
                .setRetryAttempts(p.getRetryAttempts())
                .setRetryInterval(p.getRetryInterval())
                .setIdleConnectionTimeout(p.getIdleConnectionTimeout())
                .setPingConnectionInterval(p.getPingTimeout());
        if (props.getPassword() != null && !props.getPassword().isBlank()) server.setPassword(props.getPassword());
        if (props.getUsername() != null && !props.getUsername().isBlank()) server.setUsername(props.getUsername());
    }

    static void configureSentinel(Config config, PeachRedissonProperties props,
                                          PeachRedissonProperties.RedissonOptions p, String[] addresses) {
        SentinelServersConfig server = config.useSentinelServers()
                .setMasterName(props.getSentinelMaster())
                .addSentinelAddress(addresses)
                .setDatabase(props.getDatabase())
                .setMasterConnectionPoolSize(p.getMasterConnectionPoolSize())
                .setMasterConnectionMinimumIdleSize(p.getMasterConnectionMinimumIdleSize())
                .setSlaveConnectionPoolSize(p.getSlaveConnectionPoolSize())
                .setSlaveConnectionMinimumIdleSize(p.getSlaveConnectionMinimumIdleSize())
                .setTimeout(p.getTimeout())
                .setConnectTimeout(p.getConnectTimeout())
                .setRetryAttempts(p.getRetryAttempts())
                .setRetryInterval(p.getRetryInterval())
                .setIdleConnectionTimeout(p.getIdleConnectionTimeout())
                .setPingConnectionInterval(p.getPingTimeout())
                .setSubscriptionsPerConnection(p.getSubscriptionsPerConnection());
        if (props.getPassword() != null && !props.getPassword().isBlank()) server.setPassword(props.getPassword());
        if (props.getUsername() != null && !props.getUsername().isBlank()) server.setUsername(props.getUsername());
        if (props.getSentinelUsername() != null && !props.getSentinelUsername().isBlank()) {
            server.setSentinelUsername(props.getSentinelUsername());
        }
        if (props.getSentinelPassword() != null && !props.getSentinelPassword().isBlank()) {
            server.setSentinelPassword(props.getSentinelPassword());
        }
    }

    static void configureCluster(Config config, PeachRedissonProperties props,
                                         PeachRedissonProperties.RedissonOptions p, String[] addresses) {
        if (props.getDatabase() != 0) {
            throw new IllegalArgumentException("Redis Cluster only supports database 0");
        }
        ClusterServersConfig server = config.useClusterServers()
                .addNodeAddress(addresses)
                .setScanInterval(p.getScanInterval())
                .setMasterConnectionPoolSize(p.getMasterConnectionPoolSize())
                .setMasterConnectionMinimumIdleSize(p.getMasterConnectionMinimumIdleSize())
                .setSlaveConnectionPoolSize(p.getSlaveConnectionPoolSize())
                .setSlaveConnectionMinimumIdleSize(p.getSlaveConnectionMinimumIdleSize())
                .setTimeout(p.getTimeout())
                .setConnectTimeout(p.getConnectTimeout())
                .setRetryAttempts(p.getRetryAttempts())
                .setRetryInterval(p.getRetryInterval())
                .setIdleConnectionTimeout(p.getIdleConnectionTimeout())
                .setPingConnectionInterval(p.getPingTimeout())
                .setSubscriptionsPerConnection(p.getSubscriptionsPerConnection());
        if (props.getPassword() != null && !props.getPassword().isBlank()) server.setPassword(props.getPassword());
        if (props.getUsername() != null && !props.getUsername().isBlank()) server.setUsername(props.getUsername());
    }
}
