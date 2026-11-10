package com.peach.redis.stream.config;

import com.peach.redis.stream.MessageConsumer;
import com.peach.redis.stream.RedisStreamHandler;
import com.peach.redis.stream.RedisStreamLinstener;
import com.peach.redis.stream.RedisStreamPushHandler;
import com.peach.redis.stream.RedisStreamPendingRecovery;
import com.peach.redis.stream.constant.RedisStreamContant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

/**
 * Redis流Auto配置类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/18 16:12
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(name = {
        "com.peach.redis.common.RedisConfig",
        "com.peach.redis.autoconfigure.PeachRedisAutoConfiguration"
})
@EnableConfigurationProperties({RedisStreamProperties.class, RedisStreamRecoveryProperties.class})
@ConditionalOnProperty(prefix = RedisStreamContant.REDIS_STREAM_PREFIX, value = "enable", havingValue = "true")
public class RedisStreamAutoConfig {

    @Bean
    @ConditionalOnMissingBean(RedisStreamPushHandler.class)
    public RedisStreamPushHandler redisStreamPushHandler(StringRedisTemplate stringRedisTemplate, RedisStreamProperties redisStreamProperties){
        return new RedisStreamPushHandler(redisStreamProperties,stringRedisTemplate);
    }


    @Bean
    @ConditionalOnMissingBean(RedisStreamHandler.class)
    public RedisStreamHandler redisStreamHandler(StringRedisTemplate stringRedisTemplate, RedisStreamPushHandler redisStreamPushHandler){
        return new RedisStreamHandler(redisStreamPushHandler,stringRedisTemplate);
    }

    @Bean(name = "streamMessageListenerContainer", destroyMethod = "stop")
    @ConditionalOnBean(MessageConsumer.class)
    @ConditionalOnMissingBean(name = "streamMessageListenerContainer")
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> streamMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            RedisStreamProperties redisStreamProperties,
            RedisStreamHandler redisStreamHandler,
            MessageConsumer messageConsumer,
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("redisStreamExecutor") ThreadPoolExecutor executor) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>>
                options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(5))
                .batchSize(10)
                .targetType(String.class)
                .errorHandler(t -> log.error("StreamMessageListenerContainerOptions build error", t))
                .executor(executor)
                .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> container = StreamMessageListenerContainer.create(redisConnectionFactory, options);
        checkConsumerType(redisStreamProperties.getConsumerType());
        if (RedisStreamContant.GROUP.equals(redisStreamProperties.getConsumerType())) {
            redisStreamHandler.streamBindingGroup(redisStreamProperties.getStreamName(), redisStreamProperties.getConsumerGroup());
            String consumerInstance = redisStreamProperties.getConsumerName() + "-"
                    + UUID.randomUUID().toString().substring(0, 8);
            RedisStreamLinstener listener = new RedisStreamLinstener(
                    messageConsumer, stringRedisTemplate, redisStreamProperties.getConsumerGroup());
            container.receive(
                    Consumer.from(redisStreamProperties.getConsumerGroup(), consumerInstance),
                    StreamOffset.create(redisStreamProperties.getStreamName(), ReadOffset.lastConsumed()),
                    listener);
        } else {
            container.receive(StreamOffset.fromStart(redisStreamProperties.getStreamName()),
                    new RedisStreamLinstener(messageConsumer));
        }
        container.start();
        return container;
    }

    /**
     * 超时 Pending 认领属于可选可靠消费能力，依赖 group 已创建和业务消费端存在。
     * 不影响原有 Redis Stream / broadcast 使用方的启动。
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn("streamMessageListenerContainer")
    @ConditionalOnBean(MessageConsumer.class)
    @ConditionalOnMissingBean(RedisStreamPendingRecovery.class)
    @ConditionalOnProperty(prefix = "peach.redis.stream.recovery", name = "enabled", havingValue = "true")
    public RedisStreamPendingRecovery redisStreamPendingRecovery(StringRedisTemplate stringRedisTemplate,
                                                                RedisStreamProperties streamProperties,
                                                                RedisStreamRecoveryProperties recoveryProperties,
                                                                MessageConsumer messageConsumer) {
        if (!RedisStreamContant.GROUP.equals(streamProperties.getConsumerType())) {
            throw new IllegalArgumentException("Redis Stream Pending recovery requires consumer-type=group");
        }
        return new RedisStreamPendingRecovery(stringRedisTemplate, streamProperties, recoveryProperties, messageConsumer);
    }

    @Bean(name = "redisStreamExecutor", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "redisStreamExecutor")
    public ThreadPoolExecutor redisStreamExecutor() {
        return createThreadPool();
    }

    private ThreadPoolExecutor createThreadPool(){
        int coreThreadCount = Runtime.getRuntime().availableProcessors();
        AtomicInteger threadCount = new AtomicInteger(1);
        return new ThreadPoolExecutor(
                coreThreadCount,
                2 * coreThreadCount,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("thread-consumer-stream-task-" + threadCount.getAndIncrement());
                    return thread;
                });
    }
    private void checkConsumerType(String consumerType){
        if ((!RedisStreamContant.GROUP.equals(consumerType)) && (!RedisStreamContant.BROADCAST.equals(consumerType))) {
            throw new IllegalStateException("checkConsumerType error");
        }
    }
}
