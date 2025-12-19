package com.peach.redis.stream;

import com.peach.redis.stream.config.RedisStreamProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/18 16:30
 */
@Slf4j
public class RedisStreamPushHandler {

    private final RedisStreamProperties redisStreamProperties;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisStreamPushHandler(RedisStreamProperties redisStreamProperties, StringRedisTemplate stringRedisTemplate) {
        this.redisStreamProperties = redisStreamProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 推送消息
     * @param message
     * @return
     */
    public RecordId push(String message){
        ObjectRecord<String, String> record = StreamRecords.newRecord()
                .in(redisStreamProperties.getStreamName())
                .ofObject(message)
                .withId(RecordId.autoGenerate());
        RecordId recordId = this.stringRedisTemplate.opsForStream().add(record);
        log.info("redis streamName : {} message : {}", redisStreamProperties.getStreamName(),message);
        return recordId;
    }
}
