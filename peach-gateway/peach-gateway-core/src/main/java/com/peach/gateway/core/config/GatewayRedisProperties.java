package com.peach.gateway.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 网关读取的共享 Redis 连接参数。
 *
 * <p>地址、密码、库号和模式来自 {@code peach-redis.yml}。网关只用这些参数创建 Lettuce 连接，
 * 不加载 Jedis 或 Redisson。{@code peach.redis.redisson.ssl-enable} 只决定 Lettuce 是否启用 SSL。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/06
 */
@Data
@ConfigurationProperties(prefix = "peach.redis")
public class GatewayRedisProperties {

    /** 单机模式。 */
    public static final String STANDALONE = "standalone";

    /** 哨兵模式。 */
    public static final String SENTINEL = "sentinel";

    /** 集群模式。 */
    public static final String CLUSTER = "cluster";

    /**
     * 连接模式，取值为 standalone、sentinel 或 cluster。
     */
    private String mode;

    /**
     * 节点地址。单机写一个 {@code host:port}，哨兵和集群用逗号分隔多个节点。
     */
    private String host;

    /**
     * Redis 密码。空白表示不设置密码。
     */
    private String password = "";

    /**
     * 哨兵主节点名称。
     */
    private String sentinelMaster = "master";

    /**
     * 逻辑库编号。
     */
    private int database;

    /**
     * 共享配置中的 Redisson 段。网关只读取 SSL 开关。
     */
    private SharedClient redisson = new SharedClient();

    /**
     * 当前连接是否启用 SSL。
     *
     * @return {@code peach.redis.redisson.ssl-enable} 为 true 时返回 true
     */
    public boolean isSslEnabled() {
        return redisson != null && redisson.isSslEnable();
    }

    /**
     * 构建单机连接配置。多个节点时沿用共享配置的既有行为，使用最后一个节点。
     *
     * @return 单机配置
     */
    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        Iterator<RedisNode> nodes = nodes().iterator();
        RedisNode selected = nodes.next();
        while (nodes.hasNext()) {
            selected = nodes.next();
        }
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(selected.getHost());
        configuration.setPort(selected.getPort() == null ? 6379 : selected.getPort());
        applyPassword(configuration);
        configuration.setDatabase(database);
        return configuration;
    }

    /**
     * 构建哨兵连接配置。
     *
     * @return 哨兵配置
     */
    public RedisSentinelConfiguration sentinelConfiguration() {
        RedisSentinelConfiguration configuration = new RedisSentinelConfiguration();
        configuration.master(sentinelMaster);
        configuration.setSentinels(nodes());
        applyPassword(configuration);
        configuration.setDatabase(database);
        return configuration;
    }

    /**
     * 构建集群连接配置。
     *
     * @return 集群配置
     */
    public RedisClusterConfiguration redisClusterConfiguration() {
        RedisClusterConfiguration configuration = new RedisClusterConfiguration();
        configuration.setClusterNodes(nodes());
        configuration.setMaxRedirects(5);
        applyPassword(configuration);
        return configuration;
    }

    private Set<RedisNode> nodes() {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("peach.redis.host is required");
        }
        Set<RedisNode> nodes = new HashSet<>();
        String[] nodeText = host.replaceAll("\\s*", "").split(",");
        for (String node : nodeText) {
            if (node.isEmpty()) {
                continue;
            }
            String[] parts = node.split(":");
            if (parts.length != 2 || parts[0].isEmpty()) {
                throw new IllegalArgumentException("Invalid peach.redis.host node: " + node);
            }
            int port = Integer.parseInt(parts[1]);
            nodes.add(new RedisNode(parts[0], port));
        }
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("peach.redis.host is required");
        }
        return nodes;
    }

    private void applyPassword(RedisConfiguration.WithPassword configuration) {
        if (password != null && !password.isBlank()) {
            configuration.setPassword(RedisPassword.of(password));
        }
    }

    /**
     * 共享配置 {@code peach.redis.redisson} 中网关会读取的字段。
     */
    @Data
    public static class SharedClient {

        /**
         * 是否对 Redis 连接启用 SSL。
         */
        private boolean sslEnable;
    }
}
