package com.peach.redis.autoconfigure;


import com.peach.redis.common.RedisConfig;
import com.peach.redis.listener.CacheMessageListener;
import com.peach.redis.config.MultiCacheConfig;
import com.peach.redis.manager.MultiCacheManager;
import com.peach.redis.manager.MultiCacheManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Objects;

@Slf4j
@AutoConfigureAfter(RedisConfig.class)
@EnableConfigurationProperties(MultiCacheConfig.class)
@ConditionalOnProperty(prefix = "peach.multicache", name = "enabled", matchIfMissing = true)
public class MultiCacheAutoConfiguration<K, V>{

    @Bean
    @DependsOn("redisTemplate")
    @ConditionalOnMissingBean(MultiCacheManager.class)
    public MultiCacheManager cacheManager(RedisTemplate<K, V> redisTemplate, MultiCacheConfig cacheConfig) {
        log.info("init MultiCacheManager successful");
        return new MultiCacheManager(redisTemplate, cacheConfig);
    }


    @Bean
    @DependsOn("redisTemplate")
    @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
    public RedisMessageListenerContainer cacheListenerContainer(MultiCacheConfig cacheConfig, RedisTemplate<K, V> redisTemplate, MultiCacheManager cacheManager) {
        log.info("init RedisMessageListenerContainer successful");
        RedisMessageListenerContainer cacheListenerContainer = new RedisMessageListenerContainer();
        cacheListenerContainer.setConnectionFactory(Objects.requireNonNull(redisTemplate.getConnectionFactory()));
        CacheMessageListener<K, V> cacheMessageListener = new CacheMessageListener<>(redisTemplate, cacheManager);
        cacheListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(cacheConfig.getRedis().getTopic()));
        return cacheListenerContainer;
    }

    /**
     * 缓存服务 通过cacheManager操作缓存的工具类实现类
     *
     * @param cacheManager
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(MultiCacheManagerService.class)
    public MultiCacheManagerService cacheManagerService(MultiCacheManager cacheManager) {
        log.info("init MultiCacheManagerService successful");
        return new MultiCacheManagerService(cacheManager);
    }


}
