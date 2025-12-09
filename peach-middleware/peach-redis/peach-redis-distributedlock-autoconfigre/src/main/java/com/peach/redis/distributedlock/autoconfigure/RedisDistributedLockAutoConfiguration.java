package com.peach.redis.distributedlock.autoconfigure;

import com.peach.redis.distributedlock.RedisDistributedLock;
import com.peach.redis.distributedlock.RedisDistributedLockProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

@AutoConfiguration
@EnableConfigurationProperties(RedisDistributedLockProperties.class)
@ConditionalOnProperty(prefix = "redis.lock", name = "enabled", matchIfMissing = true)
public class RedisDistributedLockAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    public RedisDistributedLock redisDistributedLock(RedisTemplate<String, Object> redisTemplate,
                                                     RedisDistributedLockProperties properties) {
        return new RedisDistributedLock(redisTemplate, properties);
    }
}

