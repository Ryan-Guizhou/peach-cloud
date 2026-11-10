package com.peach.redis.listener;

import com.peach.redis.manager.MultiCacheManager;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 重订阅确认仅清理本地缓存，且重复通知不影响数据协议。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
class CacheResubscriptionTest {

    @Test
    void eachSubscriptionConfirmationInvalidatesLocalCachesWithoutRedisWrites() {
        MultiCacheManager manager = mock(MultiCacheManager.class);
        CacheMessageListener listener = new CacheMessageListener(manager);

        listener.onChannelSubscribed("cache:invalidations".getBytes(java.nio.charset.StandardCharsets.UTF_8), 1);
        listener.onChannelSubscribed("cache:invalidations".getBytes(java.nio.charset.StandardCharsets.UTF_8), 1);

        verify(manager, times(2)).clearAllLocal();
        org.mockito.Mockito.verifyNoMoreInteractions(manager);
    }
}
