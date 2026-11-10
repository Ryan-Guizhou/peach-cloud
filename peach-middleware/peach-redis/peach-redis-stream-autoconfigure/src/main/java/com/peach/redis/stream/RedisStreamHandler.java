package com.peach.redis.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

/**
 * RedisStream 处理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:29
 * @Description RedisStream 处理器
 */
@Slf4j
public class RedisStreamHandler {
    
    private final RedisStreamPushHandler redisStreamPushHandler;
    
    private final StringRedisTemplate stringRedisTemplate;

    public RedisStreamHandler(RedisStreamPushHandler redisStreamPushHandler, StringRedisTemplate stringRedisTemplate) {
        this.redisStreamPushHandler = redisStreamPushHandler;
        this.stringRedisTemplate = stringRedisTemplate;
    }
    
    public void addGroup(String streamName, String groupName){
        stringRedisTemplate.opsForStream().createGroup(streamName,groupName);
    }
    
    public Boolean hasKey(String key){
        if(Objects.isNull(key)){
            return false;
        }else{
            return stringRedisTemplate.hasKey(key);
        }
        
    }
 
    public void del(String key, RecordId recordIds){
        stringRedisTemplate.opsForStream().delete(key,recordIds);
    }
    
    /**
     * 即使 stream 已存在也需要确保 group 存在。多实例并发初始化时 BUSYGROUP 可以忽略。
     */
    public void streamBindingGroup(String streamName, String group) {
        RecordId bootstrap = null;
        if (!Boolean.TRUE.equals(hasKey(streamName))) {
            bootstrap = stringRedisTemplate.opsForStream().add(
                    org.springframework.data.redis.connection.stream.StreamRecords.newRecord()
                            .in(streamName).ofObject("{\"bootstrap\":true}"));
        }
        try {
            addGroup(streamName, group);
        } catch (RuntimeException exception) {
            if (!isGroupAlreadyExists(exception)) {
                throw exception;
            }
            log.debug("Redis Stream group already exists, stream={}, group={}", streamName, group);
        } finally {
            if (bootstrap != null) {
                del(streamName, bootstrap);
            }
        }
    }

    private boolean isGroupAlreadyExists(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("BUSYGROUP")) {
                return true;
            }
        }
        return false;
    }

}