package com.peach.redission.delayqueue.config;

import com.peach.redis.common.RedisConfig;
import com.peach.redission.delayqueue.context.DelayQueueBasePart;
import com.peach.redission.delayqueue.context.DelayQueueContext;
import com.peach.redission.delayqueue.event.DelayQueueInitHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 17:30
 */
@AutoConfiguration
@AutoConfigureAfter(RedisConfig.class)
@EnableConfigurationProperties(DelayQueueProperties.class)
public class DelayQueueAutoConfig {


    @Bean
    @ConditionalOnMissingBean(DelayQueueBasePart.class)
    public DelayQueueBasePart delayQueueBasePart(RedissonClient redissonClient, DelayQueueProperties delayQueueProperties){
        DelayQueueBasePart delayQueueBasePart = new DelayQueueBasePart(redissonClient,delayQueueProperties);
        return delayQueueBasePart;
    }


    @Bean
    @ConditionalOnMissingBean(DelayQueueInitHandler.class)
    public DelayQueueInitHandler delayQueueInitHandler(DelayQueueBasePart delayQueueBasePart){
        DelayQueueInitHandler delayQueueInitHandler = new DelayQueueInitHandler(delayQueueBasePart);
        return delayQueueInitHandler;
    }

    @Bean
    @ConditionalOnMissingBean(DelayQueueContext.class)
    public DelayQueueContext delayQueueContext(DelayQueueBasePart delayQueueBasePart){
        DelayQueueContext delayQueueContext = new DelayQueueContext(delayQueueBasePart);
        return delayQueueContext;
    }
}
