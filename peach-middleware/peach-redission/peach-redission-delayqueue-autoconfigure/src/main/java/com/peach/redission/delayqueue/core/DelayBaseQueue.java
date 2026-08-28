package com.peach.redission.delayqueue.core;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;


/**
 * 延迟队列基础类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 16:53
 * @Description 延迟队列基础类
 */
@Slf4j
public class DelayBaseQueue {
    
    /**
     * Redisson客户端实例
     */
    protected final RedissonClient redissonClient;
    
    /**
     * Redisson阻塞队列实例
     */
    protected final RBlockingQueue<String> blockingQueue;
    
    
    /**
     * 构造函数
     * 
     * @param redissonClient Redisson客户端实例
     * @param relTopic 队列主题名称
     */
    public DelayBaseQueue(RedissonClient redissonClient, String relTopic){
        this.redissonClient = redissonClient;
        this.blockingQueue = redissonClient.getBlockingQueue(relTopic);
        log.debug("Initialized DelayBaseQueue with topic: {}", relTopic);
    }
}