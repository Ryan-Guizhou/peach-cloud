package com.peach.redis.stream.config;

import com.peach.redis.common.RedisConfig;
import com.peach.redis.stream.MessageConsumer;
import com.peach.redis.stream.RedisStreamHandler;
import com.peach.redis.stream.RedisStreamLinstener;
import com.peach.redis.stream.RedisStreamPushHandler;
import com.peach.redis.stream.constant.RedisStreamContant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
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

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/18 16:12
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter(RedisConfig.class)
@EnableConfigurationProperties(RedisStreamProperties.class)
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

    @Bean
    @ConditionalOnBean(MessageConsumer.class)
    @ConditionalOnMissingBean(StreamMessageListenerContainer.class)
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> streamMessageListenerContainer(
            JedisConnectionFactory jedisConnectionFactory,
            RedisStreamProperties redisStreamProperties,
            RedisStreamHandler redisStreamHandler,
            MessageConsumer messageConsumer) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>>
                options = StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(5))
                .batchSize(10)
                .targetType(String.class)
                .errorHandler(t -> log.error("StreamMessageListenerContainerOptions build error", t))
                .executor(createThreadPool())
                .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> container = StreamMessageListenerContainer.create(jedisConnectionFactory, options);
        checkConsumerType(redisStreamProperties.getConsumerType());
        RedisStreamLinstener redisStreamListener = new RedisStreamLinstener(messageConsumer);
        if (RedisStreamContant.GROUP.equals(redisStreamProperties.getConsumerType())) {
            redisStreamHandler.streamBindingGroup(redisStreamProperties.getStreamName(), redisStreamProperties.getConsumerGroup());
            container.receiveAutoAck(
                    Consumer.from(redisStreamProperties.getConsumerGroup(), redisStreamProperties.getConsumerName()),
                    StreamOffset.create(redisStreamProperties.getStreamName(), ReadOffset.lastConsumed()),
                    redisStreamListener);
        } else {
            container.receive(StreamOffset.fromStart(redisStreamProperties.getStreamName()), redisStreamListener);
        }
        container.start();
        return container;
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
            throw new RuntimeException("checkConsumerType error");
        }
    }
}
