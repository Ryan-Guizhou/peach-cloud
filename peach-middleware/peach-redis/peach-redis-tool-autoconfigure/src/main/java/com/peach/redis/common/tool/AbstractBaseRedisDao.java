
package com.peach.redis.common.tool;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 抽象基础Redis数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:39
 */
public abstract class AbstractBaseRedisDao<K, V> {

    protected final RedisTemplate<K, V> redisTemplate;

    protected AbstractBaseRedisDao(RedisTemplate<K, V> redisTemplate) {
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringSerializer);
        redisTemplate.setValueSerializer(stringSerializer);
        redisTemplate.setHashKeySerializer(stringSerializer);
        redisTemplate.setHashValueSerializer(stringSerializer);
        redisTemplate.setStringSerializer(stringSerializer);
        redisTemplate.setDefaultSerializer(stringSerializer);
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取 RedisSerializer
     */
    protected RedisSerializer<String> getRedisSerializer() {
        return redisTemplate.getStringSerializer();
    }
}
