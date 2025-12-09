/*
 *  Project Name: DataPlus PRO Platform
 *  Copyright (C) 2023 Datatang (Beijing) Technology Co., Ltd. (https://www.datatang.com) All rights reserved.
 *  Notice: This content is intended for internal circulation within Datatang (Beijing) Technology Co., Ltd. only. Unauthorized disclosure,copying, distribution, or use for any other commercial purposes isstrictly prohibited.
 */

package com.peach.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:39
 */
public abstract class AbstractBaseRedisDao<K, V> {

    @Resource(name = "redisTemplate")
    protected RedisTemplate<K, V> redisTemplate;

    /**
     * 设置redisTemplate
     *
     * @param redisTemplate the redisTemplate to set
     */
    public void setRedisTemplate(RedisTemplate<K, V> redisTemplate) {
        RedisSerializer stringSerializer = new StringRedisSerializer();
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
