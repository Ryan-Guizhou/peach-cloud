package com.peach.redission.common;

import com.peach.redis.common.RedisConfig;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/19 18:59
 */
@AutoConfiguration
@AutoConfigureAfter(RedisConfig.class)
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
