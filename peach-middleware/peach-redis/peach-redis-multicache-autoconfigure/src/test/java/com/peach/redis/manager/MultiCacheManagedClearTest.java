package com.peach.redis.manager;

import com.peach.virtualthread.registry.VirtualExecutorRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证异步缓存清理只经由受管虚拟线程执行器提交任务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class MultiCacheManagedClearTest {

    @Test
    void shouldRejectUnconfiguredManagedGroup() {
        MultiCacheManager manager = mock(MultiCacheManager.class);
        MultiCacheManagerService service = new MultiCacheManagerService(manager);
        assertThatThrownBy(() -> service.asyncClear("products"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("multicache-clear");
    }

    @Test
    void shouldSubmitToManagedGroupAndPropagateRejection() {
        MultiCacheManager manager = mock(MultiCacheManager.class);
        VirtualExecutorRegistry registry = mock(VirtualExecutorRegistry.class);
        when(registry.groupNames()).thenReturn(Set.of("multicache-clear"));
        MultiCacheManagerService service = new MultiCacheManagerService(manager, registry);

        service.asyncClear("products");
        verify(registry).execute(eq("multicache-clear"), any(Runnable.class));

        doThrow(new RejectedExecutionException("capacity exceeded"))
                .when(registry).execute(eq("multicache-clear"), any(Runnable.class));
        assertThatThrownBy(() -> service.asyncClear("products"))
                .isInstanceOf(RejectedExecutionException.class);
    }
}
