
package com.peach.redis.tool;

import org.springframework.data.redis.core.RedisTemplate;

/**
 * Redis DAO 的模板持有基类，不创建独立连接或负责关闭共享模板。
 *
 * @param <K> 模板键类型，由应用配置的键序列化器决定实际支持类型
 * @param <V> 模板值类型，由应用配置的值序列化器负责转换
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:39
 */
public abstract class AbstractBaseRedisDao<K, V> {

    protected final RedisTemplate<K, V> redisTemplate;

    protected AbstractBaseRedisDao(RedisTemplate<K, V> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
