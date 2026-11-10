package com.peach.redission.delayqueue.config;

import com.peach.redission.delayqueue.context.DelayQueueBasePart;
import com.peach.redission.delayqueue.v2.DelayQueueV2Properties;
import com.peach.redission.delayqueue.v2.RedisDelayQueueV2;
import com.peach.redission.delayqueue.context.DelayQueueContext;
import com.peach.redission.delayqueue.event.DelayQueueInitHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 延迟队列Auto配置类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 17:30
 */
@AutoConfiguration
@AutoConfigureAfter(name = "com.peach.redisson.autoconfigure.PeachRedissonAutoConfiguration")
@EnableConfigurationProperties({DelayQueueProperties.class, DelayQueueV2Properties.class})
public class DelayQueueAutoConfig {


    @Bean
    @ConditionalOnProperty(prefix = "peach.delay.queue", name = "legacy-enabled",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(DelayQueueBasePart.class)
    public DelayQueueBasePart delayQueueBasePart(RedissonClient redissonClient, DelayQueueProperties delayQueueProperties){
        return new DelayQueueBasePart(redissonClient, delayQueueProperties);
    }


    @Bean
    @ConditionalOnProperty(prefix = "peach.delay.queue", name = "legacy-enabled",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(DelayQueueInitHandler.class)
    public DelayQueueInitHandler delayQueueInitHandler(DelayQueueBasePart delayQueueBasePart){
        return new DelayQueueInitHandler(delayQueueBasePart);
    }

    @Bean
    @ConditionalOnProperty(prefix = "peach.delay.queue", name = "legacy-enabled",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(DelayQueueContext.class)
    public DelayQueueContext delayQueueContext(DelayQueueBasePart delayQueueBasePart){
        return new DelayQueueContext(delayQueueBasePart);
    }
    /**
     * 独立 V2 状态机默认禁用；只有显式配置时加载脚本、创建轮询器。
     * 与旧队列 key 完全隔离，支持单独关闭 legacy beans。
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "peach.delay.queue.v2", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(RedisDelayQueueV2.class)
    public RedisDelayQueueV2 redisDelayQueueV2(RedissonClient redissonClient,
                                               DelayQueueV2Properties properties) {
        return new RedisDelayQueueV2(redissonClient, properties);
    }

}
