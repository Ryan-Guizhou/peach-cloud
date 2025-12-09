

package com.peach.redis;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:13
 * @Description 多级缓存
 */
@Slf4j
@Data
@Configuration
public class RedisConfig<K, V> {

    @Value("${spring.redis.mode}")
    private String mode;

    @Value("${spring.redis.host}")
    private String host;

    @Value("${spring.redis.password}")
    private String password;

    @Value("${spring.redis.sentinelMaster:master}")
    private String sentinelMaster;

    @Value("#{${spring.redis.database:0}}")
    private int database;

    @Value("${spring.redis.redisson.enabled:true}")
    private boolean redissonEnabled;

    @Value("${spring.redis.redisson.threads:16}")
    private int redissonThreads;

    @Value("${spring.redis.redisson.netty-threads:32}")
    private int redissonNettyThreads;

    @Value("${spring.redis.redisson.timeout:3000}")
    private int redissonTimeout;

    @Value("${spring.redis.redisson.connection-pool-size:64}")
    private int redissonConnectionPoolSize;

    @Value("${spring.redis.redisson.connection-minimum-idle-size:10}")
    private int redissonConnectionMinimumIdleSize;

    @Value("${spring.redis.redisson.subscription-connection-pool-size:50}")
    private int redissonSubscriptionConnectionPoolSize;

    @Value("${spring.redis.redisson.subscription-connection-minimum-idle-size:1}")
    private int redissonSubscriptionConnectionMinimumIdleSize;

    @Value("${spring.redis.redisson.slave-connection-minimum-idle-size:10}")
    private int slaveConnectionMinimumIdleSize;

    @Value("${spring.redis.redisson.master-connection-minimum-idle-size:10}")
    private int masterConnectionMinimumIdleSize;

    @Value("${spring.redis.redisson.slave-connection-pool-size:64}")
    private int slaveConnectionPoolSize;

    @Value("${spring.redis.redisson.master-connection-pool-size:64}")
    private int masterConnectionPoolSize;

    @Value("${spring.redis.redisson.scan-interval:1000}")
    private int scanInterval;

    @Value("${spring.redis.redisson.idle-connection-timeout:10000}")
    private int idleConnectionTimeout;

    @Value("${spring.redis.redisson.ping-timeout:1000}")
    private int pingTimeout;

    @Value("${spring.redis.redisson.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${spring.redis.redisson.retry-attempts:3}")
    private int retryAttempts;

    @Value("${spring.redis.redisson.retry-interval:1500}")
    private int retryInterval;

    @Value("${spring.redis.redisson.subscriptions-per-connection:5}")
    private int subscriptionsPerConnection;

    @Value("${spring.redis.redisson.ssl-enable:false}")
    private boolean sslEnable;



    @Bean(name = "redisPoolConfigs")
    @ConditionalOnMissingBean
    public JedisPoolConfig poolConfig() {
        JedisPoolConfig pool = new JedisPoolConfig();
        pool.setMaxTotal(600);
        pool.setMinIdle(1);
        pool.setMaxIdle(10);
        pool.setMaxWait(Duration.ofSeconds(60));
        pool.setTestWhileIdle(true);
        pool.setTestOnBorrow(true);
        pool.setMinEvictableIdleTime(Duration.ofSeconds(60));
        pool.setTimeBetweenEvictionRuns(Duration.ofSeconds(60));
        pool.setNumTestsPerEvictionRun(100);
        return pool;
    }

    @Bean(name = "jedisConnectionFactory")
    @ConditionalOnMissingBean
    JedisConnectionFactory jedisConnectionFactory(
            @Qualifier("redisPoolConfigs") JedisPoolConfig jedisPoolConfig) {
        JedisConnectionFactory jedisConnectionFactory = null;
        log.info("=============== redis host:" + host);
        //单机模式
        switch (mode) {
            case MultiCacheConstant.STANDALONE:
                //获得默认的连接池构造
                //JedisConnectionFactory对于Standalone模式的没有（RedisStandaloneConfiguration，JedisPoolConfig）的构造函数，对此
                //我们用JedisClientConfiguration接口的builder方法实例化一个构造器，还得类型转换
                JedisClientConfiguration.JedisPoolingClientConfigurationBuilder jpcf = (JedisClientConfiguration.JedisPoolingClientConfigurationBuilder) JedisClientConfiguration.builder();
                //修改我们的连接池配置
                jpcf.poolConfig(jedisPoolConfig);
                //通过构造器来构造jedis客户端配置
                JedisClientConfiguration jedisClientConfiguration = jpcf.build();
                jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration(), jedisClientConfiguration);
                log.info("redis standalone mode  init success！");
                break;
            case MultiCacheConstant.SENTINEL:
                //哨兵模式
                jedisConnectionFactory = new JedisConnectionFactory(sentinelConfiguration(), jedisPoolConfig);
                log.info("redis sentinel mode  init success！");
                break;
            case MultiCacheConstant.CLUSTER:
                //Cluster模式
                jedisConnectionFactory = new JedisConnectionFactory(redisClusterConfiguration(), jedisPoolConfig);
                log.info("redis cluster mode  init success！");
                break;
            default:
                break;
        }
        return jedisConnectionFactory;
    }


    @Bean(name = "redisTemplate")
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory);
        //序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);
        template.setHashValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    private Set<RedisNode> getNodes(String nodesStr) {
        Set<RedisNode> nodes = new HashSet<>();
        String[] nodeStr = nodesStr.replaceAll("\\s*", "").split(",");
        IntStream.range(0, nodeStr.length).forEach(i -> {
            String hostName = nodeStr[i].split(":")[0];
            int port = Integer.parseInt(nodeStr[i].split(":")[1]);
            RedisNode node = new RedisNode(hostName, port);
            nodes.add(node);
        });
        return nodes;
    }

    public RedisStandaloneConfiguration redisStandaloneConfiguration() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        Set<RedisNode> nodes = getNodes(host);
        Iterator<RedisNode> nodesIterator = nodes.iterator();
        String hostName = null;
        Integer port = 0;
        while (nodesIterator.hasNext()) {
            RedisNode redisNode = nodesIterator.next();
            port = redisNode.getPort();
            hostName = redisNode.getHost();

        }
        assert hostName != null;
        redisStandaloneConfiguration.setHostName(hostName);
        redisStandaloneConfiguration.setPort(port == null ? 6379 : port);
        if (!StringUtils.isBlank(password)) {
            redisStandaloneConfiguration.setPassword(RedisPassword.of(password));
        }
        redisStandaloneConfiguration.setDatabase(database);
        return redisStandaloneConfiguration;
    }

    public RedisClusterConfiguration redisClusterConfiguration() {
        RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
        Set<RedisNode> nodes = getNodes(host);
        redisClusterConfiguration.setClusterNodes(nodes);
        redisClusterConfiguration.setMaxRedirects(5);
        if (!StringUtils.isBlank(password)) {
            redisClusterConfiguration.setPassword(RedisPassword.of(password));
        }
        return redisClusterConfiguration;
    }

    public RedisSentinelConfiguration sentinelConfiguration() {
        RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration();
        //配置master的名称
        redisSentinelConfiguration.master(sentinelMaster);
        //配置redis的哨兵sentinel
        Set<RedisNode> nodes = getNodes(host);
        redisSentinelConfiguration.setSentinels(nodes);
        if (!StringUtils.isBlank(password)) {
            redisSentinelConfiguration.setPassword(RedisPassword.of(password));
        }
        redisSentinelConfiguration.setDatabase(database);
        return redisSentinelConfiguration;
    }

    // 添加 RedissonClient 配置
    @Primary
    @Bean(name = "redissonClient", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        if (!redissonEnabled) {
            log.info("Redisson is disabled, skip initialization");
            return null;
        }

        try {
            Config config = new Config();

            // 设置线程池和Netty配置
            config.setThreads(redissonThreads);
            config.setNettyThreads(redissonNettyThreads);

            switch (mode) {
                case MultiCacheConstant.STANDALONE:
                    config = configureSingleServer(config);
                    log.info("Redisson standalone mode init success!");
                    break;
                case MultiCacheConstant.SENTINEL:
                    config = configureSentinelServers(config);
                    log.info("Redisson sentinel mode init success!");
                    break;
                case MultiCacheConstant.CLUSTER:
                    config = configureClusterServers(config);
                    log.info("Redisson cluster mode init success!");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported redis mode: " + mode);
            }

            // 设置编码器（默认使用JsonJacksonCodec）
            config.setCodec(new org.redisson.codec.JsonJacksonCodec());

            // 设置看门狗超时时间（分布式锁相关）
            config.setLockWatchdogTimeout(30000);

            RedissonClient redisson = Redisson.create(config);
            log.info("Redisson client initialized successfully with mode: {}", mode);
            return redisson;

        } catch (Exception e) {
            log.error("Failed to initialize Redisson client", e);
            throw new RuntimeException("Failed to initialize Redisson client", e);
        }
    }

    private Config configureSingleServer(Config config) {
        Set<RedisNode> nodes = getNodes(host);
        Iterator<RedisNode> iterator = nodes.iterator();
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("No redis nodes found for standalone mode");
        }

        RedisNode node = iterator.next();
        String address = "redis://" + node.getHost() + ":" + node.getPort();

        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setConnectionPoolSize(redissonConnectionPoolSize)
                .setConnectionMinimumIdleSize(redissonConnectionMinimumIdleSize)
                .setSubscriptionConnectionPoolSize(redissonSubscriptionConnectionPoolSize)
                .setSubscriptionConnectionMinimumIdleSize(redissonSubscriptionConnectionMinimumIdleSize)
                .setTimeout(redissonTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setPingConnectionInterval(pingTimeout)
                .setConnectTimeout(connectTimeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval);

        if (StringUtils.isNotBlank(password)) {
            serverConfig.setPassword(password);
        }

        if (sslEnable) {
            serverConfig.setAddress("rediss://" + node.getHost() + ":" + node.getPort());
        }

        return config;
    }

    private Config configureSentinelServers(Config config) {
        Set<RedisNode> nodes = getNodes(host);
        String[] sentinelAddresses = nodes.stream()
                .map(node -> "redis://" + node.getHost() + ":" + node.getPort())
                .toArray(String[]::new);

        SentinelServersConfig serverConfig = config.useSentinelServers()
                .setMasterName(sentinelMaster)
                .addSentinelAddress(sentinelAddresses)
                .setDatabase(database)
                .setMasterConnectionPoolSize(masterConnectionPoolSize)
                .setMasterConnectionMinimumIdleSize(masterConnectionMinimumIdleSize)
                .setSlaveConnectionPoolSize(slaveConnectionPoolSize)
                .setSlaveConnectionMinimumIdleSize(slaveConnectionMinimumIdleSize)
                .setTimeout(redissonTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setPingConnectionInterval(pingTimeout)
                .setConnectTimeout(connectTimeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setSubscriptionsPerConnection(subscriptionsPerConnection);

        if (StringUtils.isNotBlank(password)) {
            serverConfig.setPassword(password);
        }

        if (sslEnable) {
            String[] sslAddresses = nodes.stream()
                    .map(node -> "rediss://" + node.getHost() + ":" + node.getPort())
                    .toArray(String[]::new);
            serverConfig.addSentinelAddress(sslAddresses);
        }

        return config;
    }

    private Config configureClusterServers(Config config) {
        Set<RedisNode> nodes = getNodes(host);
        String[] nodeAddresses = nodes.stream()
                .map(node -> "redis://" + node.getHost() + ":" + node.getPort())
                .toArray(String[]::new);

        ClusterServersConfig serverConfig = config.useClusterServers()
                .addNodeAddress(nodeAddresses)
                .setScanInterval(scanInterval)
                .setMasterConnectionPoolSize(masterConnectionPoolSize)
                .setMasterConnectionMinimumIdleSize(masterConnectionMinimumIdleSize)
                .setSlaveConnectionPoolSize(slaveConnectionPoolSize)
                .setSlaveConnectionMinimumIdleSize(slaveConnectionMinimumIdleSize)
                .setTimeout(redissonTimeout)
                .setIdleConnectionTimeout(idleConnectionTimeout)
                .setPingConnectionInterval(pingTimeout)
                .setConnectTimeout(connectTimeout)
                .setRetryAttempts(retryAttempts)
                .setRetryInterval(retryInterval)
                .setSubscriptionsPerConnection(subscriptionsPerConnection);

        if (StringUtils.isNotBlank(password)) {
            serverConfig.setPassword(password);
        }

        if (sslEnable) {
            String[] sslAddresses = nodes.stream()
                    .map(node -> "rediss://" + node.getHost() + ":" + node.getPort())
                    .toArray(String[]::new);
            serverConfig.addNodeAddress(sslAddresses);
        }

        return config;
    }
}
