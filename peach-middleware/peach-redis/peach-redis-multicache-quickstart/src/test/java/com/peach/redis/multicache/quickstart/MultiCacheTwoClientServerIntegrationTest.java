package com.peach.redis.multicache.quickstart;

import com.peach.redis.config.MultiCacheConfig;
import com.peach.redis.listener.CacheMessageListener;
import com.peach.redis.manager.MultiCache;
import com.peach.redis.manager.MultiCacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.cache.Cache;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 两个独立 Jedis 连接与本地缓存验证 Pub/Sub 失效及 L2 读取。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CLIENT_IT", matches = "true")
class MultiCacheTwoClientServerIntegrationTest {

    @Test
    void peerUpdateInvalidatesLocalL1AndNextReadFindsNewValue() throws InterruptedException {
        JedisConnectionFactory firstFactory = factory();
        JedisConnectionFactory secondFactory = factory();
        RedisMessageListenerContainer listener = null;
        String prefix = "twoclient:cache:" + UUID.randomUUID();
        String key = "SKU-777";
        String redisKey = prefix + ":products:" + key;
        StringRedisTemplate firstRedis = new StringRedisTemplate(firstFactory);
        firstRedis.afterPropertiesSet();
        StringRedisTemplate secondRedis = new StringRedisTemplate(secondFactory);
        secondRedis.afterPropertiesSet();

        try {
            MultiCacheConfig config = new MultiCacheConfig();
            config.setCacheNames(Set.of("products"));
            config.setCachePrefix(prefix);
            config.getRedis().setTopic(prefix + ":invalidations");
            MultiCacheManager first = new MultiCacheManager(firstRedis, config);
            MultiCacheManager second = new MultiCacheManager(secondRedis, config);
            Cache writer = first.getCache("products");
            Cache reader = second.getCache("products");

            listener = new RedisMessageListenerContainer();
            listener.setConnectionFactory(secondFactory);
            listener.addMessageListener(new CacheMessageListener(second),
                    new ChannelTopic(config.getRedis().getTopic()));
            listener.afterPropertiesSet();
            listener.start();

            writer.put(key, "before");
            // Wait until the first publication is processed before priming the remote L1.
            Thread.sleep(300);
            assertThat(reader.get(key)).isNotNull();
            assertThat(reader.get(key).get()).isEqualTo("before");
            assertThat(((MultiCache) reader).getLocalCache().getIfPresent(key)).isEqualTo("before");

            writer.put(key, "after");
            boolean invalidated = false;
            for (int attempt = 0; attempt < 100; attempt++) {
                if (((MultiCache) reader).getLocalCache().getIfPresent(key) == null) {
                    invalidated = true;
                    break;
                }
                Thread.sleep(50);
            }
            assertThat(invalidated).as("Remote Pub/Sub listener should invalidate L1").isTrue();
            assertThat(reader.get(key).get()).isEqualTo("after");
        } finally {
            firstRedis.delete(redisKey);
            if (listener != null) {
                listener.stop();
                listener.destroy();
            }
            secondFactory.destroy();
            firstFactory.destroy();
        }
    }

    private static JedisConnectionFactory factory() {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration("127.0.0.1", 6379);
        configuration.setDatabase(0);
        JedisConnectionFactory result = new JedisConnectionFactory(configuration);
        result.afterPropertiesSet();
        return result;
    }
}
