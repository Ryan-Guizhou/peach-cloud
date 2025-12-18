package com.peach.redission.delayqueue.core;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 16:53
 * @Description 死信队列管理器
 * 用于处理多次重试失败的消息，将这些消息存储到专门的死信队列中，以便后续分析和处理
 */
@Slf4j
public class DeadLetterQueueManager {
    
    /**
     * Redisson客户端实例
     */
    private final RedissonClient redissonClient;
    
    /**
     * 死信队列大小限制，防止无限增长
     */
    private final Integer maxDeadLetterQueueSize;
    
    /**
     * 死信队列清理间隔（小时）
     */
    private final Integer cleanIntervalHours;
    
    /**
     * 死信队列消息保留时间（小时）
     */
    private final Integer messageRetentionHours;
    
    /**
     * 死信队列消息计数器，用于跟踪每个主题的死信消息数量
     */
    private final Map<String, AtomicInteger> deadLetterCounters = new ConcurrentHashMap<>();
    
    /**
     * 定时清理任务执行器
     */
    private ScheduledExecutorService scheduledExecutorService;
    
    /**
     * 已注册的主题列表，用于定期清理
     */
    private final Set<String> registeredTopics = ConcurrentHashMap.newKeySet();
    
    /**
     * 构造函数（使用默认配置）
     * 
     * @param redissonClient Redisson客户端实例
     */
    public DeadLetterQueueManager(RedissonClient redissonClient) {
        this(redissonClient, 10000, 24, 168); // 默认配置
    }
    
    /**
     * 构造函数
     * 
     * @param redissonClient Redisson客户端实例
     * @param maxDeadLetterQueueSize 死信队列最大大小
     * @param cleanIntervalHours 清理间隔（小时）
     * @param messageRetentionHours 消息保留时间（小时）
     */
    public DeadLetterQueueManager(RedissonClient redissonClient, int maxDeadLetterQueueSize, 
                                int cleanIntervalHours, int messageRetentionHours) {
        this.redissonClient = redissonClient;
        this.maxDeadLetterQueueSize = maxDeadLetterQueueSize;
        this.cleanIntervalHours = cleanIntervalHours;
        this.messageRetentionHours = messageRetentionHours;
        initScheduledCleanup();
    }
    
    /**
     * 初始化定时清理任务
     */
    private void initScheduledCleanup() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "dead-letter-queue-cleanup"));
        
        // 定时清理过期消息
        scheduledExecutorService.scheduleAtFixedRate(
                this::cleanupExpiredMessages, 
                cleanIntervalHours, 
                cleanIntervalHours, 
                TimeUnit.HOURS);
    }
    
    /**
     * 清理过期消息
     */
    private void cleanupExpiredMessages() {
        try {
            log.info("Starting periodic cleanup of expired dead letter messages");
            // 对所有已注册的主题进行清理
            for (String topic : registeredTopics) {
                cleanOldMessages(topic, messageRetentionHours);
            }
            log.info("Periodic cleanup completed");
        } catch (Exception e) {
            log.error("Error during periodic cleanup of dead letter messages", e);
        }
    }
    
    /**
     * 将处理失败的消息移动到死信队列
     *  当消息处理失败次数超过最大重试次数时，调用此方法将消息存储到死信队列中。
     * 如果死信队列已满，将移除最旧的消息
     * 
     * @param topic 原始消息主题
     * @param content 消息内容
     * @param exception 异常信息
     * @param retryCount 重试次数
     */
    public void moveToDeadLetterQueue(String topic, String content, Exception exception, int retryCount) {
        try {
            String deadLetterTopic = topic + "-dead-letter";
            
            // 注册主题以便定期清理
            registeredTopics.add(topic);
            
            // 检查死信队列大小，如果超过限制则清理
            AtomicInteger counter = deadLetterCounters.computeIfAbsent(deadLetterTopic, k -> new AtomicInteger(0));
            if (counter.get() >= maxDeadLetterQueueSize) {
                log.warn("Dead letter queue for topic {} is full. Current size: {}", deadLetterTopic, counter.get());
                // 清理最旧的消息以腾出空间
                cleanOldestMessages(deadLetterTopic, 10); // 清理最旧的10条消息
            }
            
            RMap<String, Object> deadLetterMap = redissonClient.getMap(deadLetterTopic);
            
            Map<String, Object> deadLetterInfo = new HashMap<>();
            deadLetterInfo.put("content", content);
            deadLetterInfo.put("exception", exception.getClass().getName() + ": " + exception.getMessage());
            deadLetterInfo.put("retryCount", retryCount);
            deadLetterInfo.put("timestamp", LocalDateTime.now().toString());
            
            // 使用时间戳作为key，确保唯一性
            String key = System.currentTimeMillis() + "-" + System.nanoTime();
            deadLetterMap.put(key, deadLetterInfo);
            
            counter.incrementAndGet();
            log.info("Message moved to dead letter queue. Topic: {}, Retry Count: {}, Queue Size: {}", 
                    deadLetterTopic, retryCount, counter.get());
        } catch (Exception e) {
            log.error("Failed to move message to dead letter queue. Topic: {}", topic, e);
        }
    }
    
    /**
     * 清理死信队列中最旧的消息
     * 
     * @param deadLetterTopic 死信队列主题
     * @param count 要清理的消息数量
     */
    private void cleanOldestMessages(String deadLetterTopic, int count) {
        try {
            RMap<String, Object> deadLetterMap = redissonClient.getMap(deadLetterTopic);
            
            // 获取所有键并按时间排序
            Set<String> keys = deadLetterMap.keySet();
            if (keys.isEmpty()) {
                return;
            }
            
            // 将键转换为列表并按时间排序（假设键包含时间戳）
            List<String> sortedKeys = new ArrayList<>(keys);
            Collections.sort(sortedKeys);
            
            // 删除最旧的count条消息
            int removedCount = 0;
            for (String key : sortedKeys) {
                if (removedCount >= count) {
                    break;
                }
                deadLetterMap.remove(key);
                removedCount++;
            }
            
            // 更新计数器
            AtomicInteger counter = deadLetterCounters.get(deadLetterTopic);
            if (counter != null) {
                counter.addAndGet(-removedCount);
            }
            
            log.info("Cleaned {} oldest messages from dead letter queue: {}", removedCount, deadLetterTopic);
        } catch (Exception e) {
            log.error("Failed to clean oldest messages from dead letter queue: {}", deadLetterTopic, e);
        }
    }
    
    /**
     * 清理死信队列中的旧消息
     * 
     * @param topic 原始消息主题
     * @param maxAgeHours 最大保留小时数
     */
    public void cleanOldMessages(String topic, int maxAgeHours) {
        try {
            String deadLetterTopic = topic + "-dead-letter";
            RMap<String, Object> deadLetterMap = redissonClient.getMap(deadLetterTopic);
            
            // 计算过期时间阈值
            LocalDateTime expireTime = LocalDateTime.now().minusHours(maxAgeHours);
            
            // 获取所有条目并检查时间戳
            Map<String, Object> allEntries = deadLetterMap.readAllMap();
            int removedCount = 0;
            
            for (Map.Entry<String, Object> entry : allEntries.entrySet()) {
                try {
                    if (entry.getValue() instanceof Map) {
                        Map<?, ?> valueMap = (Map<?, ?>) entry.getValue();
                        Object timestampObj = valueMap.get("timestamp");
                        
                        if (timestampObj instanceof String) {
                            LocalDateTime messageTime = LocalDateTime.parse((String) timestampObj);
                            if (messageTime.isBefore(expireTime)) {
                                deadLetterMap.remove(entry.getKey());
                                removedCount++;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse timestamp for entry: {}", entry.getKey(), e);
                }
            }
            
            // 更新计数器
            AtomicInteger counter = deadLetterCounters.get(deadLetterTopic);
            if (counter != null) {
                counter.addAndGet(-removedCount);
            }
            
            log.info("Cleaned {} old messages from dead letter queue for topic: {}", removedCount, deadLetterTopic);
        } catch (Exception e) {
            log.error("Failed to clean old messages from dead letter queue. Topic: {}", topic, e);
        }
    }
    
    /**
     * 关闭资源
     */
    @PreDestroy
    public void destroy() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
            try {
                if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("DeadLetterQueueManager cleanup scheduler shut down");
        }
    }
}