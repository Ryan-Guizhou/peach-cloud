package com.peach.redission.delayqueue.core;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 16:53
 * @Description 延迟队列生产者类
 * 负责将消息添加到Redisson的延迟队列中，消息将在指定的延迟时间后被消费者处理
 */
@Slf4j
public class DelayProduceQueue extends DelayBaseQueue {
    
    /**
     * Redisson延迟队列实例
     */
    private final RDelayedQueue<String> delayedQueue;

    /**
     * 构造函数
     * 
     * @param redissonClient Redisson客户端实例
     * @param relTopic 队列主题名称
     */
    public DelayProduceQueue(RedissonClient redissonClient, final String relTopic) {
        super(redissonClient, relTopic);
        this.delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        log.debug("Initialized DelayProduceQueue with topic: {}", relTopic);
    }
    
    /**
     * 向延迟队列中添加消息
     * 
     * <p>消息将在指定的延迟时间后被消费者处理。</p>
     * 
     * @param content 消息内容
     * @param delayTime 延迟时间
     * @param timeUnit 延迟时间单位
     */
    public void offer(String content, long delayTime, TimeUnit timeUnit) {
        delayedQueue.offer(content,delayTime,timeUnit);
        log.debug("Offered message to delayed queue: {} with delay: {} {}", content, delayTime, timeUnit);
    }
}