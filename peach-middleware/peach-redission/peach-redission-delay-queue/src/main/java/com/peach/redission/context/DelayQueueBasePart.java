package com.peach.redission.context;

import com.peach.redission.config.DelayQueueProperties;
import lombok.Data;
import org.redisson.api.RedissonClient;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 17:28
 * @Description 延迟队列基础配置信息
 */
@Data
public class DelayQueueBasePart {

    private final RedissonClient redissonClient;

    private final DelayQueueProperties delayQueueProperties;

    public DelayQueueBasePart(RedissonClient redissonClient, DelayQueueProperties delayQueueProperties) {
        this.redissonClient = redissonClient;
        this.delayQueueProperties = delayQueueProperties;
    }
}
