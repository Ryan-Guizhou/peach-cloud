package com.peach.redission.common;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * RedissionCommonAutoconfigure相关类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/19 18:59
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "com.peach.redis.common.RedisConfig",
        "com.peach.redisson.autoconfigure.PeachRedissonAutoConfiguration"
})
public class RedissionCommonAutoconfigure {

    @Bean
    @ConditionalOnMissingBean(LocalCacheLock.class)
    public LocalCacheLock localCacheLock(){
        return new LocalCacheLock();
    }

    @Bean
    @ConditionalOnMissingBean(RedissionDataHandle.class)
    public RedissionDataHandle redissionDataHandle(RedissonClient redissonClient){
        return new RedissionDataHandle(redissonClient);
    }

    @Bean
    @ConditionalOnMissingBean(LockInfoHandleFactory.class)
    public LockInfoHandleFactory lockInfoHandleFactory(){
        return new LockInfoHandleFactory();
    }
}
