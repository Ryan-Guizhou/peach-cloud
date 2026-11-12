package com.peach.redis.autoconfigure;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Indexed;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Spring Data Redis / Jedis 独立底座；不引用或创建 RedissonClient。
 * 用户已提供 RedisConnectionFactory 时退让，不再依赖旧混合 RedisConfig。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@Slf4j
@Indexed
@AutoConfiguration(before = RedisAutoConfiguration.class)
@ConditionalOnClass({RedisTemplate.class, JedisConnectionFactory.class})
@ConditionalOnProperty(prefix = "peach.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachRedisProperties.class)
public class PeachRedisAutoConfiguration {

    @Bean(name = "redisPoolConfigs")
    @ConditionalOnMissingBean(JedisPoolConfig.class)
    public JedisPoolConfig redisPoolConfigs(PeachRedisProperties props) {
        PeachRedisProperties.JedisPool settings = props.getJedis();
        validateParam(settings);
        JedisPoolConfig pool = new JedisPoolConfig();
        pool.setMaxTotal(settings.getMaxTotal());
        pool.setMinIdle(settings.getMinIdle());
        pool.setMaxIdle(settings.getMaxIdle());
        pool.setMaxWait(Duration.ofMillis(settings.getMaxWaitMillis()));
        pool.setTestOnBorrow(true);
        pool.setTestWhileIdle(true);
        return pool;
    }

    @Bean(name = "jedisConnectionFactory")
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public JedisConnectionFactory jedisConnectionFactory(JedisPoolConfig jedisPoolConfigs, PeachRedisProperties props) {
        JedisClientConfiguration.JedisClientConfigurationBuilder builder = JedisClientConfiguration.builder();
        builder.usePooling().poolConfig(jedisPoolConfigs);
        if (props.isSslEnabled()) {
            builder.useSsl();
        }
        JedisClientConfiguration client = builder.build();
        Set<RedisNode> nodes = parseNodes(props.getHost());
        if (!StringUtils.hasText(props.getMode())) {
            log.error("Validation failed: parameter [peach.redis.mode] is required but missing.");
            throw new IllegalArgumentException("peach.redis.mode is required");
        }
        return switch (props.getMode()) {
            case "standalone" -> new JedisConnectionFactory(standalone(props, nodes), client);
            case "sentinel" -> new JedisConnectionFactory(sentinel(props, nodes), client);
            case "cluster" -> new JedisConnectionFactory(cluster(props, nodes), client);
            default -> {
                log.error("Validation failed: unsupported redis mode [{}]", props.getMode());
                throw new IllegalArgumentException("Unsupported peach.redis.mode: " + props.getMode());
            }
        };
    }

    @Bean(name = "redisTemplate")
    @ConditionalOnMissingBean(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Keep exact legacy Jackson2JsonRedisSerializer semantics for stored values.
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean(name = "stringRedisTemplate")
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 解析配置中的主机节点列表（支持逗号分隔的 host:port 格式）。
     *
     * @param hostList 主机列表字符串
     * @return RedisNode 集合
     */
    private static Set<RedisNode> parseNodes(String hostList) {
        if (!StringUtils.hasText(hostList)) {
            log.error("Validation failed: parameter [peach.redis.host] must contain at least one host:port.");
            throw new IllegalArgumentException("peach.redis.host must contain at least one host:port");
        }
        Set<RedisNode> nodes = new LinkedHashSet<>();
        for (String entry : Arrays.asList(hostList.split(","))) {
            String address = entry.trim();
            String[] parts = address.split(":");
            if (parts.length != 2) {
                log.error("Validation failed: invalid Redis address format [{}]", address);
                throw new IllegalArgumentException("Invalid Redis address: " + address);
            }
            int port;
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                log.error("Validation failed: invalid Redis port number in address [{}]", address, exception);
                throw new IllegalArgumentException("Invalid Redis port: " + address, exception);
            }
            if (port <= 0 || port > 65535 || parts[0].isBlank()) {
                log.error("Validation failed: out-of-range port or blank host in address [{}]", address);
                throw new IllegalArgumentException("Invalid Redis address: " + address);
            }
            nodes.add(new RedisNode(parts[0], port));
        }
        return nodes;
    }

    /**
     * 构建单机模式 Redis 配置。
     * @param props redis相关配置
     * @param nodes 节点信息
     */
    private static RedisStandaloneConfiguration standalone(PeachRedisProperties props, Set<RedisNode> nodes) {
        if (nodes.size() != 1) {
            log.error("Validation failed: Standalone Redis requires exactly a single host, but found [{}].", nodes.size());
            throw new IllegalArgumentException("Standalone Redis requires a single host");
        }
        RedisNode node = nodes.iterator().next();
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(node.getHost(), node.getPort());
        cfg.setDatabase(props.getDatabase());
        applyAuth(cfg, props);
        return cfg;
    }


    /**
     * 构建哨兵模式 Redis 配置。
     * @param props redis相关配置
     * @param nodes 节点信息
     */
    static RedisSentinelConfiguration sentinel(PeachRedisProperties props, Set<RedisNode> nodes) {
        RedisSentinelConfiguration cfg = new RedisSentinelConfiguration();
        cfg.master(props.getSentinelMaster());
        cfg.setSentinels(nodes);
        cfg.setDatabase(props.getDatabase());

        // 认证信息通常为可选配置，使用 Optional 过滤，有值时才绑定
        Optional.ofNullable(props.getUsername())
                .filter(StringUtils::hasText)
                .ifPresent(cfg::setUsername);
        Optional.ofNullable(props.getPassword())
                .filter(StringUtils::hasText)
                .ifPresent(pwd -> cfg.setPassword(RedisPassword.of(pwd)));
        Optional.ofNullable(props.getSentinelUsername())
                .filter(StringUtils::hasText)
                .ifPresent(cfg::setUsername);
        Optional.ofNullable(props.getSentinelPassword())
                .filter(StringUtils::hasText)
                .ifPresent(pwd -> cfg.setSentinelPassword(RedisPassword.of(pwd)));
        return cfg;
    }

    /**
     * 构建集群模式 Redis 配置。
     * @param props redis相关配置
     * @param nodes 节点信息
     */
    static RedisClusterConfiguration cluster(PeachRedisProperties props, Set<RedisNode> nodes) {
        if (props.getDatabase() != 0) {
            log.error("Validation failed: Redis Cluster only supports database index 0, but configured [{}]", props.getDatabase());
            throw new IllegalArgumentException("Redis Cluster only supports database 0");
        }
        RedisClusterConfiguration cfg = new RedisClusterConfiguration();
        cfg.setClusterNodes(nodes);
        cfg.setMaxRedirects(5);

        // 集群密码/账号可选绑定
        Optional.ofNullable(props.getUsername())
                .filter(StringUtils::hasText)
                .ifPresent(cfg::setUsername);
        Optional.ofNullable(props.getPassword())
                .filter(StringUtils::hasText)
                .ifPresent(pwd -> cfg.setPassword(RedisPassword.of(pwd)));
        return cfg;
    }

    /**
     * 应用单机模式认证配置。
     */
    private static void applyAuth(RedisStandaloneConfiguration cfg, PeachRedisProperties props) {
        Optional.ofNullable(props)
                .ifPresent(p -> {
                    Optional.ofNullable(p.getUsername())
                            .filter(StringUtils::hasText)
                            .ifPresent(cfg::setUsername);
                    Optional.ofNullable(p.getPassword())
                            .filter(StringUtils::hasText)
                            .ifPresent(pwd -> cfg.setPassword(RedisPassword.of(pwd)));
                });
    }


    /**
     * 校验 Jedis 连接池配置参数是否合法。
     * @param settings jedis 连接池配置
     */
    private static void validateParam(PeachRedisProperties.JedisPool settings){
        if (settings == null
                || settings.getMaxTotal() <= 0 || settings.getMaxIdle() < 0 || settings.getMinIdle() < 0
                || settings.getMaxIdle() > settings.getMaxTotal()
                || settings.getMinIdle() > settings.getMaxIdle()) {
            log.error("Validation failed: invalid peach.redis.jedis pool limits configuration.");
            throw new IllegalArgumentException("Invalid peach.redis.jedis pool limits");
        }
    }
}
