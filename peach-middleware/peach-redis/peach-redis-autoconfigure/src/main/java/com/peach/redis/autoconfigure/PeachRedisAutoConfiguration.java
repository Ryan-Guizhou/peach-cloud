package com.peach.redis.autoconfigure;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
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
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Spring Data Redis / Jedis 独立底座；不引用或创建 RedissonClient。
 * 历史 RedisConfig 若存在则先运行，本配置优先使用其连接工厂与模板。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@AutoConfiguration(before = RedisAutoConfiguration.class)
@AutoConfigureAfter(name = "com.peach.redis.common.RedisConfig")
@ConditionalOnClass({RedisTemplate.class, JedisConnectionFactory.class})
@ConditionalOnProperty(prefix = "peach.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachRedisProperties.class)
public class PeachRedisAutoConfiguration {

    @Bean(name = "redisPoolConfigs")
    @ConditionalOnMissingBean(JedisPoolConfig.class)
    public JedisPoolConfig redisPoolConfigs(PeachRedisProperties props) {
        PeachRedisProperties.JedisPool settings = props.getJedis();
        if (settings.getMaxTotal() <= 0 || settings.getMaxIdle() < 0 || settings.getMinIdle() < 0
                || settings.getMaxIdle() > settings.getMaxTotal()
                || settings.getMinIdle() > settings.getMaxIdle()) {
            throw new IllegalArgumentException("Invalid peach.redis.jedis pool limits");
        }
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
        if (props.getMode() == null) {
            throw new IllegalArgumentException("peach.redis.mode is required");
        }
        return switch (props.getMode()) {
            case "standalone" -> new JedisConnectionFactory(standalone(props, nodes), client);
            case "sentinel" -> new JedisConnectionFactory(sentinel(props, nodes), client);
            case "cluster" -> new JedisConnectionFactory(cluster(props, nodes), client);
            default -> throw new IllegalArgumentException("Unsupported peach.redis.mode: " + props.getMode());
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

    private static Set<RedisNode> parseNodes(String hostList) {
        if (hostList == null || hostList.isBlank()) {
            throw new IllegalArgumentException("peach.redis.host must contain at least one host:port");
        }
        Set<RedisNode> nodes = new LinkedHashSet<>();
        for (String entry : Arrays.asList(hostList.split(","))) {
            String address = entry.trim();
            String[] parts = address.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid Redis address: " + address);
            }
            int port;
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid Redis port: " + address, exception);
            }
            if (port <= 0 || port > 65535 || parts[0].isBlank()) {
                throw new IllegalArgumentException("Invalid Redis address: " + address);
            }
            nodes.add(new RedisNode(parts[0], port));
        }
        return nodes;
    }

    private static RedisStandaloneConfiguration standalone(PeachRedisProperties props, Set<RedisNode> nodes) {
        if (nodes.size() != 1) {
            throw new IllegalArgumentException("Standalone Redis requires a single host");
        }
        RedisNode node = nodes.iterator().next();
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(node.getHost(), node.getPort());
        cfg.setDatabase(props.getDatabase());
        applyAuth(cfg, props);
        return cfg;
    }

    static RedisSentinelConfiguration sentinel(PeachRedisProperties props, Set<RedisNode> nodes) {
        RedisSentinelConfiguration cfg = new RedisSentinelConfiguration();
        cfg.master(props.getSentinelMaster());
        cfg.setSentinels(nodes);
        cfg.setDatabase(props.getDatabase());
        if (props.getUsername() != null && !props.getUsername().isBlank()) {
            cfg.setUsername(props.getUsername());
        }
        if (props.getPassword() != null && !props.getPassword().isBlank()) {
            cfg.setPassword(RedisPassword.of(props.getPassword()));
        }
        if (props.getSentinelUsername() != null && !props.getSentinelUsername().isBlank()) {
            cfg.setSentinelUsername(props.getSentinelUsername());
        }
        if (props.getSentinelPassword() != null && !props.getSentinelPassword().isBlank()) {
            cfg.setSentinelPassword(RedisPassword.of(props.getSentinelPassword()));
        }
        return cfg;
    }

    static RedisClusterConfiguration cluster(PeachRedisProperties props, Set<RedisNode> nodes) {
        if (props.getDatabase() != 0) {
            throw new IllegalArgumentException("Redis Cluster only supports database 0");
        }
        RedisClusterConfiguration cfg = new RedisClusterConfiguration();
        cfg.setClusterNodes(nodes);
        cfg.setMaxRedirects(5);
        if (props.getUsername() != null && !props.getUsername().isBlank()) {
            cfg.setUsername(props.getUsername());
        }
        if (props.getPassword() != null && !props.getPassword().isBlank()) {
            cfg.setPassword(RedisPassword.of(props.getPassword()));
        }
        return cfg;
    }

    private static void applyAuth(RedisStandaloneConfiguration cfg, PeachRedisProperties props) {
        if (props.getUsername() != null && !props.getUsername().isBlank()) {
            cfg.setUsername(props.getUsername());
        }
        if (props.getPassword() != null && !props.getPassword().isBlank()) {
            cfg.setPassword(RedisPassword.of(props.getPassword()));
        }
    }
}
