package com.peach.redission.distributedlock.quickstart;

import com.peach.redission.distrbutedlock.manage.DistrbutedLockerManager;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证分布式锁管理器不会跨 RedissonClient 实例共享锁适配器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11
 */
class DistributedLockerIsolationTest {

    @Test
    void shouldKeepEachManagerBoundToItsOwnClient() {
        RedissonClient firstClient = mock(RedissonClient.class);
        RedissonClient secondClient = mock(RedissonClient.class);
        RLock firstLock = mock(RLock.class);
        RLock secondLock = mock(RLock.class);

        when(firstClient.getLock("resource")).thenReturn(firstLock);
        when(secondClient.getLock("resource")).thenReturn(secondLock);

        DistrbutedLockerManager first = new DistrbutedLockerManager(firstClient);
        DistrbutedLockerManager second = new DistrbutedLockerManager(secondClient);

        assertSame(firstLock, first.getReentrantLocker().getLock("resource"));
        assertSame(secondLock, second.getReentrantLocker().getLock("resource"));
        assertSame(firstLock, first.getReentrantLocker().getLock("resource"));
    }
}
