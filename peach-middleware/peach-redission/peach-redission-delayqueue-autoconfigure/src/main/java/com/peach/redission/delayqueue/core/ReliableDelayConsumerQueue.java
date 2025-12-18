package com.peach.redission.delayqueue.core;

import com.peach.redission.delayqueue.context.DelayQueuePart;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 17:37
 */
@Slf4j
public class ReliableDelayConsumerQueue extends DelayBaseQueue{

    private final AtomicInteger listenStartThreadCount = new AtomicInteger(1);

    private final AtomicInteger executeTaskThreadCount = new AtomicInteger(1);

    private final ThreadPoolExecutor listenStartThreadPool;

    private final ThreadPoolExecutor executeTaskThreadPool;

    private final AtomicBoolean runFlag = new AtomicBoolean(false);

    private final ConsumerTask consumerTask;
    
    private final DeadLetterQueueManager deadLetterQueueManager;
    
    private final int maxRetryAttempts;
    
    private final long retryIntervalMillis;

    private final RMap<String, ProcessingMessage> processingMessages;

    public ReliableDelayConsumerQueue(DelayQueuePart delayQueuePart, String relTopic){
        super(delayQueuePart.getDelayQueueBasePart().getRedissonClient(), relTopic);
        this.listenStartThreadPool = new ThreadPoolExecutor(1,1,60,
                TimeUnit.SECONDS,new LinkedBlockingQueue<>(), r -> new Thread(Thread.currentThread().getThreadGroup(), r,
                "reliable-listen-start-thread-" + listenStartThreadCount.getAndIncrement()));
        this.executeTaskThreadPool = new ThreadPoolExecutor(
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getCorePoolSize(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getMaximumPoolSize(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getKeepAliveTime(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getUnit(),
                new LinkedBlockingQueue<>(delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getWorkQueueSize()),
                r -> new Thread(Thread.currentThread().getThreadGroup(), r,
                        "reliable-delay-queue-consume-thread-" + executeTaskThreadCount.getAndIncrement()));
        this.consumerTask = delayQueuePart.getConsumerTask();
        
        // 初始化死信队列管理器
        this.deadLetterQueueManager = new DeadLetterQueueManager(
                delayQueuePart.getDelayQueueBasePart().getRedissonClient(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getMaxDeadLetterQueueSize(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getDeadLetterCleanIntervalHours(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getDeadLetterMessageRetentionHours());
        
        // 获取重试配置
        this.maxRetryAttempts = delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getMaxRetryAttempts();
        this.retryIntervalMillis = delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getRetryIntervalMillis();
        
        // 初始化正在处理的消息映射
        this.processingMessages = redissonClient.getMap(relTopic + ":processing");
        
        log.info("Initialized ReliableDelayConsumerQueue for topic: {} with thread pool config - core: {}, max: {}, queueSize: {}, maxRetryAttempts: {}", 
                relTopic, 
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getCorePoolSize(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getMaximumPoolSize(),
                delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties().getWorkQueueSize(),
                maxRetryAttempts);
    }

    public synchronized void startListener(){
        if (!runFlag.get()) {
            runFlag.set(true);
            log.info("Starting reliable listener for topic: {}", getTopicName());
            
            listenStartThreadPool.execute(() -> {
                // 异步执行消息恢复，不阻塞主监听线程
                recoverUnfinishedMessagesAsync();
                
                log.info("Reliable listener started for topic: {}", getTopicName());
                while (!Thread.interrupted() && runFlag.get()) {
                    try {
                        assert blockingQueue != null;
                        String content = blockingQueue.take();
                        log.debug("Received message from queue for topic: {}", getTopicName());
                        
                        // 为消息生成唯一ID
                        String messageId = UUID.randomUUID().toString();
                        // 记录消息开始处理
                        ProcessingMessage processingMessage = new ProcessingMessage(content, System.currentTimeMillis());
                        processingMessages.put(messageId, processingMessage);
                        
                        executeTaskThreadPool.execute(() -> {
                            processMessageWithAck(content, messageId);
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
                log.info("Reliable listener stopped for topic: {}", getTopicName());
            });
        }
    }
    
    /**
     * 异步恢复未完成的消息
     */
    private void recoverUnfinishedMessagesAsync() {
        executeTaskThreadPool.execute(() -> {
            try {
                log.info("Starting async recovery of unfinished messages for topic: {}", getTopicName());
                long startTime = System.currentTimeMillis();
                
                // 检查processingMessages中是否有未完成的消息
                for (String messageId : processingMessages.keySet()) {
                    ProcessingMessage processingMessage = processingMessages.get(messageId);
                    if (processingMessage != null) {
                        log.info("Recovering message: {}", messageId);
                        processMessageWithAck(processingMessage.getContent(), messageId);
                    }
                }
                
                long endTime = System.currentTimeMillis();
                log.info("Finished async recovery of unfinished messages for topic: {}, took {} ms", getTopicName(), (endTime - startTime));
            } catch (Exception e) {
                log.error("Error during async recovery of unfinished messages for topic: {}", getTopicName(), e);
            }
        });
    }
    
    /**
     * 带确认机制的消息处理方法
     * 
     * @param content 消息内容
     * @param messageId 消息ID
     */
    private void processMessageWithAck(String content, String messageId) {
        int retryCount = 0;
        boolean success = false;
        
        while (!success && retryCount <= maxRetryAttempts) {
            try {
                if (retryCount > 0) {
                    log.info("Retrying message processing. Attempt: {}/{} for topic: {}, message: {}", 
                            retryCount, maxRetryAttempts, getTopicName(), messageId);
                    // 使用指数退避策略
                    long backoffTime = retryIntervalMillis * (1L << (retryCount - 1)); // 2^(retryCount-1) * baseDelay
                    Thread.sleep(Math.min(backoffTime, 60000)); // 最大等待1分钟
                }
                
                consumerTask.execute(content);
                success = true;
                log.debug("Successfully executed consumer task for topic: {}, message: {}", getTopicName(), messageId);
            } catch (InterruptedException e) {
                log.warn("Message processing interrupted for topic: {}, message: {}", getTopicName(), messageId);
                Thread.currentThread().interrupt(); // 恢复中断状态
                break;
            } catch (Exception e) {
                retryCount++;
                log.warn("Failed to process message. Attempt: {}/{} for topic: {}, message: {}", 
                        retryCount, maxRetryAttempts, getTopicName(), messageId, e);
                
                // 如果达到最大重试次数，则移动到死信队列
                if (retryCount > maxRetryAttempts) {
                    log.error("Message processing failed after {} attempts for topic: {}, message: {}. Moving to dead letter queue.", 
                            maxRetryAttempts, getTopicName(), messageId, e);
                    deadLetterQueueManager.moveToDeadLetterQueue(getTopicName(), content, e, retryCount);
                }
            }
        }
        
        // 确认消息处理完成，从处理中映射中移除
        if (success || retryCount > maxRetryAttempts) {
            processingMessages.remove(messageId);
            if (success) {
                log.debug("Acknowledged successful message completion for topic: {}, message: {}", getTopicName(), messageId);
            } else {
                log.debug("Acknowledged failed message completion for topic: {}, message: {}", getTopicName(), messageId);
            }
        }
    }
    
    private String getTopicName() {
        // Extract topic name from blockingQueue if possible
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
        log.info("Stopping reliable listener for topic: {}", getTopicName());
        runFlag.set(false);
        
        // 关闭线程池
        destroy(listenStartThreadPool);
        destroy(executeTaskThreadPool);
        
        // 关闭死信队列管理器
        deadLetterQueueManager.destroy();
        
        log.info("Reliable listener stopped and resources released for topic: {}", getTopicName());
    }
    
    /**
     * 正在处理的消息信息
     */
    public static class ProcessingMessage {
        private String content;
        private long startTime;
        private String timestamp;
        
        public ProcessingMessage() {
            // 默认构造函数
        }
        
        public ProcessingMessage(String content, long startTime) {
            this.content = content;
            this.startTime = startTime;
            this.timestamp = LocalDateTime.now().toString();
        }
        
        // Getters and setters
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }
        
        public String getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }


}