package com.peach.redis.listener;

import com.peach.redis.manager.MultiCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;


/**
 * 缓存失效监听器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:29
 * @Description 缓存失效监听器
 */
@Slf4j
public class CacheMessageListener implements MessageListener {

    private final MultiCacheManager multiCacheManager;

    public CacheMessageListener(MultiCacheManager multiCacheManager) {
        this.multiCacheManager = multiCacheManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CacheMessage cacheMessage = CacheMessageCodec.deserialize(message.getBody());
            if (cacheMessage == null) {
                return;
            }
            multiCacheManager.clearLocal(cacheMessage.cacheName(), cacheMessage.key(), cacheMessage.sender());
        } catch (RuntimeException exception) {
            log.warn("Ignore invalid multi-cache sync message", exception);
        }
    }
}
