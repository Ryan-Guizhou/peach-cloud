package com.peach.redis.listener;

import com.peach.redis.manager.MultiCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.SubscriptionListener;


/**
 * 缓存失效监听器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:29
 * @Description 缓存失效监听器
 */
@Slf4j
public class CacheMessageListener implements MessageListener, SubscriptionListener {

    private final MultiCacheManager multiCacheManager;

    public CacheMessageListener(MultiCacheManager multiCacheManager) {
        this.multiCacheManager = multiCacheManager;
    }

    /**
     * Spring Data Redis 在首次订阅及连接恢复后的再次订阅确认时触发。
     * 由于失联期间无法接收 Pub/Sub 广播，丢弃当前 JVM 的缓存 L1，
     * 避免重连成功后仍沿用已经过期的本地值。
     *
     * @param channel 已确认订阅的频道
     * @param count 当前订阅数量
     */
    @Override
    public void onChannelSubscribed(byte[] channel, long count) {
        multiCacheManager.clearAllLocal();
        log.info("Multi-cache subscription confirmed, invalidate local L1 after subscription");
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
