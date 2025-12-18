package com.peach.redission.core;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 17:37
 */
@Slf4j
public class DelayBaseQueue {
    
    protected final RedissonClient redissonClient;
    protected final RBlockingQueue<String> blockingQueue;
    
    
    public DelayBaseQueue(RedissonClient redissonClient, String relTopic){
        this.redissonClient = redissonClient;
        this.blockingQueue = redissonClient.getBlockingQueue(relTopic);
    }
}
