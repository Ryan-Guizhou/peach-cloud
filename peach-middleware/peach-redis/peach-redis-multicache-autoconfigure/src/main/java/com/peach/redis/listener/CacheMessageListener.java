package com.peach.redis.listener;

import com.peach.redis.manager.MultiCacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:29
 */
public class CacheMessageListener<K,V> implements MessageListener {

    private RedisTemplate<K,V> redisTemplate;

    private MultiCacheManager multiCacheManager;

    public CacheMessageListener(RedisTemplate<K,V> redisTemplate,MultiCacheManager multiCacheManager) {
        this.redisTemplate = redisTemplate;
        this.multiCacheManager = multiCacheManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        CacheMessage cacheMessage = (CacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
        assert cacheMessage != null;
        multiCacheManager.clearLocal(cacheMessage.getCacheName(), cacheMessage.getKey(), cacheMessage.getSender());
    }
}
