package com.peach.redission.delayqueue.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

import static com.peach.redission.delayqueue.config.DelayQueueProperties.PREFIX;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/17 17:30
 * @Description 延迟队列属性配置类
 * 用于配置延迟队列的各种参数，包括线程池配置、分区数量和重试机制等
 */
@Data
@ConfigurationProperties(prefix = PREFIX)
public class DelayQueueProperties {

    public static final String PREFIX = "peach.delay.queue";

    /**
     * 从队列拉取数据的线程池中的核心线程数量，如果业务过慢可调大
     */
    private Integer corePoolSize = 4;
    
    /**
     * 从队列拉取数据的线程池中的最大线程数量，如果业务过慢可调大
     */
    private Integer maximumPoolSize = 4;

    /**
     * 从队列拉取数据的线程池中的最大线程回收时间
     */
    private long keepAliveTime = 30;
    
    /**
     * 从队列拉取数据的线程池中的最大线程回收时间的时间单位
     */
    private TimeUnit unit = TimeUnit.SECONDS;
    
    /**
     * 从队列拉取数据的线程池中的队列数量，如果业务过慢可调大
     */
    private Integer workQueueSize = 256;

    /**
     * 延时队列的隔离分区数，延时有瓶颈时可调大次数，但会增大redis的cpu消耗
     * (同一个topic发送者和消费者的隔离分区数必须相同)
     */
    private Integer isolationRegionCount = 5;
    
    /**
     * 消息处理失败后的最大重试次数
     */
    private Integer maxRetryAttempts = 3;
    
    /**
     * 重试间隔时间（毫秒）
     */
    private Long retryIntervalMillis = 5000L;
    
    /**
     * 死信队列最大大小，防止无限增长
     */
    private Integer maxDeadLetterQueueSize = 10000;
    
    /**
     * 死信队列清理间隔（小时）
     */
    private Integer deadLetterCleanIntervalHours = 24;
    
    /**
     * 死信队列消息保留时间（小时）
     */
    private Integer deadLetterMessageRetentionHours = 168; // 7天
    
    /**
     * 是否使用可靠队列（防止消息丢失）
     */
    private Boolean useReliableQueue = true;
}