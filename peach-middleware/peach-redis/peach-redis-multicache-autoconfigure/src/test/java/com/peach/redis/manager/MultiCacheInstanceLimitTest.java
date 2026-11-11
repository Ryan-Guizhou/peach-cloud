package com.peach.redis.manager;

import com.peach.redis.common.tool.RedisDao;
import com.peach.redis.config.MultiCacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 验证动态缓存实例容量限制在并发创建时不会超额。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class MultiCacheInstanceLimitTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldBoundConcurrentDynamicCacheCreation() throws InterruptedException {
        MultiCacheConfig config = new MultiCacheConfig();
        config.setMaxCacheInstances(2);
        MultiCacheManager manager = new MultiCacheManager(mock(RedisTemplate.class), config, mock(RedisDao.class));
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(24);
        AtomicInteger created = new AtomicInteger();
        for (int i = 0; i < 24; i++) {
            String name = "dynamic-" + i;
            Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    manager.getCache(name);
                    created.incrementAndGet();
                } catch (IllegalStateException expected) {
                    // 超过容量必须显式拒绝，不创建新缓存实例。
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        assertThat(created.get()).isEqualTo(2);
        assertThat(manager.getCacheNames()).hasSize(2);
        assertThatThrownBy(() -> manager.getCache("overflow"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPreserveConfiguredNamesBeforeLazyInstantiation() {
        MultiCacheConfig config = new MultiCacheConfig();
        config.setCacheNames(java.util.Set.of("known"));
        MultiCacheManager manager = new MultiCacheManager(mock(RedisTemplate.class), config, mock(RedisDao.class));
        assertThat(manager.getCacheNames()).containsExactly("known");
    }
}
