package com.peach.redission.delayqueue.config;

import com.peach.redission.delayqueue.context.DelayQueueContext;
import com.peach.redission.delayqueue.core.RedisDelayQueue;
import com.peach.redission.delayqueue.event.DelayQueueInitHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 统一延迟队列自动配置。只创建一套 Lua 可靠队列运行时。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@AutoConfiguration
@AutoConfigureAfter(name = "com.peach.redisson.autoconfigure.PeachRedissonAutoConfiguration")
@EnableConfigurationProperties(DelayQueueProperties.class)
@ConditionalOnProperty(prefix = DelayQueueProperties.PREFIX,
        name = "enabled", havingValue = "true", matchIfMissing = true)
public class DelayQueueAutoConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(RedisDelayQueue.class)
    public RedisDelayQueue redisDelayQueue(RedissonClient redissonClient, DelayQueueProperties properties) {
        return new RedisDelayQueue(redissonClient, properties);
    }

    @Bean
    @ConditionalOnMissingBean(DelayQueueContext.class)
    public DelayQueueContext delayQueueContext(RedisDelayQueue redisDelayQueue,
                                                DelayQueueProperties properties) {
        return new DelayQueueContext(redisDelayQueue, properties);
    }

    @Bean
    @ConditionalOnMissingBean(DelayQueueInitHandler.class)
    public DelayQueueInitHandler delayQueueInitHandler(RedisDelayQueue redisDelayQueue,
                                                       DelayQueueProperties properties) {
        return new DelayQueueInitHandler(redisDelayQueue, properties);
    }
}
