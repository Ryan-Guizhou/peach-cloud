package com.peach.redission.core;

import com.peach.redission.config.DelayQueueProperties;
import com.peach.redission.context.DelayQueueBasePart;
import com.peach.redission.context.DelayQueuePart;
import lombok.extern.slf4j.Slf4j;


import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 16:53
 * @Description 延迟队列消费者类
 * 负责从Redisson的阻塞队列中消费消息，并通过配置的线程池执行消费者任务
 */
@Slf4j
public class DelayConsumerQueue extends DelayBaseQueue{

    private final AtomicInteger listenStartThreadCount = new AtomicInteger(1);

    private final AtomicInteger executeTaskThreadCount = new AtomicInteger(1);

    private final ThreadPoolExecutor listenStartThreadPool;

    private final ThreadPoolExecutor executeTaskThreadPool;

    private final AtomicBoolean runFlag = new AtomicBoolean(false);

    private final ConsumerTask consumerTask;
    
    private final DeadLetterQueueManager deadLetterQueueManager;
    
    private final int maxRetryAttempts;
    
    private final long retryIntervalMillis;

    /**
     * 构造函数
     * 
     * @param delayQueuePart 延迟队列部分配置
     * @param relTopic 相关主题
     */
    public DelayConsumerQueue(DelayQueuePart delayQueuePart, String relTopic){

        super(delayQueuePart.getDelayQueueBasePart().getRedissonClient(), relTopic);
        DelayQueueBasePart delayQueueBasePart = delayQueuePart.getDelayQueueBasePart();
        DelayQueueProperties delayQueueProperties = delayQueueBasePart.getDelayQueueProperties();
        this.listenStartThreadPool = new ThreadPoolExecutor(1,1,60,
                TimeUnit.SECONDS,new LinkedBlockingQueue<>(), r -> new Thread(Thread.currentThread().getThreadGroup(), r,
                "listen-start-thread-" + listenStartThreadCount.getAndIncrement()));

        this.executeTaskThreadPool = new ThreadPoolExecutor(
                delayQueueProperties.getCorePoolSize(), delayQueueProperties.getMaximumPoolSize(),
                delayQueueProperties.getKeepAliveTime(), delayQueueProperties.getUnit(),
                new LinkedBlockingQueue<>(delayQueueProperties.getWorkQueueSize()),
                r -> new Thread(Thread.currentThread().getThreadGroup(), r,
                        "delay-queue-consume-thread-" + executeTaskThreadCount.getAndIncrement()));
        this.consumerTask = delayQueuePart.getConsumerTask();
        
        // 初始化死信队列管理器
        this.deadLetterQueueManager = new DeadLetterQueueManager(
                delayQueueBasePart.getRedissonClient(),
                delayQueueProperties.getMaxDeadLetterQueueSize(),
                delayQueueProperties.getDeadLetterCleanIntervalHours(),
                delayQueueProperties.getDeadLetterMessageRetentionHours());
        
        // 获取重试配置
        this.maxRetryAttempts = delayQueueProperties.getMaxRetryAttempts();
        this.retryIntervalMillis = delayQueueProperties.getRetryIntervalMillis();

    }

    public synchronized void startListener(){
        if (!runFlag.get()) {
            runFlag.set(true);
            listenStartThreadPool.execute(() -> {
                while (!Thread.interrupted() && runFlag.get()) {
                    try {
                        assert blockingQueue != null;
                        String content = blockingQueue.take();
                        log.debug("Received message from queue for topic: {}", getTopicName());
                        executeTaskThreadPool.execute(() -> {
                            processMessageWithRetry(content);
                        });
                    } catch (InterruptedException e) {
                        log.warn("Consumer listener interrupted for topic: {}", getTopicName());
                        destroy(executeTaskThreadPool);
                        Thread.currentThread().interrupt(); // 恢复中断状态
                        break;
                    } catch (Throwable e) {
                        log.error("Error taking message from blocking queue for topic: {}", getTopicName(), e);
                    }
                }
                log.info("Listener stopped for topic: {}", getTopicName());
            });
        }
    }
    
    /**
     * 带重试机制的消息处理方法
     * 
     * @param content 消息内容
     */
    private void processMessageWithRetry(String content) {
        int retryCount = 0;
        boolean success = false;
        while (!success && retryCount <= maxRetryAttempts) {
            try {
                if (retryCount > 0) {
                    log.info("Retrying message processing. Attempt: {}/{} for topic: {}", 
                            retryCount, maxRetryAttempts, getTopicName());
                    // 使用指数退避策略
                    long backoffTime = retryIntervalMillis * (1L << (retryCount - 1)); // 2^(retryCount-1) * baseDelay
                    Thread.sleep(Math.min(backoffTime, 60000)); // 最大等待1分钟
                }
                
                consumerTask.execute(content);
                success = true;
                log.debug("Successfully executed consumer task for topic: {}", getTopicName());
            } catch (InterruptedException e) {
                log.warn("Message processing interrupted for topic: {}", getTopicName());
                Thread.currentThread().interrupt(); // 恢复中断状态
                break;
            } catch (Exception e) {
                retryCount++;
                log.warn("Failed to process message. Attempt: {}/{} for topic: {}", 
                        retryCount, maxRetryAttempts, getTopicName(), e);
                
                // 如果达到最大重试次数，则移动到死信队列
                if (retryCount > maxRetryAttempts) {
                    log.error("Message processing failed after {} attempts for topic: {}. Moving to dead letter queue.", 
                            maxRetryAttempts, getTopicName(), e);
                    deadLetterQueueManager.moveToDeadLetterQueue(getTopicName(), content, e, retryCount);
                }
            }
        }
    }

    
    private String getTopicName() {
        return blockingQueue != null ? blockingQueue.getName() : "unknown";
    }

    private void destroy(ExecutorService executorService) {
        try {
            if (Objects.nonNull(executorService)) {
                executorService.shutdown();
                log.info("Executor service shutdown initiated for topic: {}", getTopicName());
                // 等待最多30秒让现有任务完成
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("Executor service did not terminate in 30 seconds for topic: {}, forcing shutdown", getTopicName());
                    executorService.shutdownNow();
                    // 再等待5秒确保关闭
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.error("Executor service did not terminate for topic: {}", getTopicName());
                    }
                } else {
                    log.info("Executor service shutdown completed for topic: {}", getTopicName());
                }
            }
        } catch (InterruptedException e) {
            log.warn("Executor service shutdown interrupted for topic: {}", getTopicName(), e);
            if (Objects.nonNull(executorService)) {
                executorService.shutdownNow();
            }
            Thread.currentThread().interrupt(); // 恢复中断状态
        } catch (Exception e) {
            log.error("Error destroying executor service for topic: {}", getTopicName(), e);
        }
    }
    
    /**
     * 停止监听器并释放资源
     */
    public void stopListener() {
        log.info("Stopping listener for topic: {}", getTopicName());
        runFlag.set(false);
        
        // 关闭线程池
        destroy(listenStartThreadPool);
        destroy(executeTaskThreadPool);
        
        // 关闭死信队列管理器
        deadLetterQueueManager.destroy();
        
        log.info("Listener stopped and resources released for topic: {}", getTopicName());
    }


}