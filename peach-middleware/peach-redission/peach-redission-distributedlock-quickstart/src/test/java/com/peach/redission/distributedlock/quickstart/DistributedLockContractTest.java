package com.peach.redission.distributedlock.quickstart;

import com.peach.redission.common.LockInfoHandle;
import com.peach.redission.common.LockInfoHandleFactory;
import com.peach.redission.common.LockInfoType;
import com.peach.redission.distrbutedlock.annoation.DistrbutedLock;
import com.peach.redission.distrbutedlock.aspect.DistrbutedLockAspect;
import com.peach.redission.distrbutedlock.locker.DistributedLocker;
import com.peach.redission.distrbutedlock.locker.LockType;
import com.peach.redission.distrbutedlock.locker.RedissionWriteLocker;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 分布式锁注解解析及锁获取语义的轻量回归测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
class DistributedLockContractTest {

    @DistrbutedLock(name = "class-rule", keys = {"class"})
    static class Example {
        public String inherited() {
            return "ok";
        }

        @DistrbutedLock(name = "method-rule", keys = {"method"})
        public String overridden() {
            return "ok";
        }
    }

    @Test
    void classAnnotationAppliesAndMethodAnnotationOverrides() throws Throwable {
        LockInfoHandleFactory handles = mock(LockInfoHandleFactory.class);
        LockInfoHandle handle = mock(LockInfoHandle.class);
        DistrbutedLockerFactory factory = mock(DistrbutedLockerFactory.class);
        DistributedLocker locker = mock(DistributedLocker.class);
        DistrbutedLockAspect aspect = new DistrbutedLockAspect(handles, factory);
        when(handles.getLockHandle(LockInfoType.DISTRIBUTE)).thenReturn(handle);
        when(handle.getLockName(any(), eq("class-rule"), any(String[].class))).thenReturn("lock:class");
        when(handle.getLockName(any(), eq("method-rule"), any(String[].class))).thenReturn("lock:method");
        when(factory.getDistrbutedLocker(LockType.REENTRANT)).thenReturn(locker);
        when(locker.tryLock(any(), eq(TimeUnit.SECONDS), eq(10L))).thenReturn(true);

        assertThat(aspect.aroundAnnotated(pointcut(Example.class.getMethod("inherited")))).isEqualTo("ok");
        assertThat(aspect.aroundAnnotated(pointcut(Example.class.getMethod("overridden")))).isEqualTo("ok");
        verify(locker).unlock("lock:class");
        verify(locker).unlock("lock:method");
        verify(handle, times(1)).getLockName(any(), eq("class-rule"), any(String[].class));
        verify(handle, times(1)).getLockName(any(), eq("method-rule"), any(String[].class));
    }

    @Test
    void writeGetLockDoesNotAcquireAndInterruptedTryLockPreservesFlag() throws Exception {
        RedissonClient client = mock(RedissonClient.class);
        RReadWriteLock readWrite = mock(RReadWriteLock.class);
        RLock lock = mock(RLock.class);
        when(client.getReadWriteLock("key")).thenReturn(readWrite);
        when(readWrite.writeLock()).thenReturn(lock);
        RedissionWriteLocker locker = new RedissionWriteLocker(client);
        assertThat(locker.getLock("key")).isSameAs(lock);
        verify(lock, never()).lock();
        when(lock.tryLock(1L, TimeUnit.SECONDS)).thenThrow(new InterruptedException("cancelled"));
        assertThat(locker.tryLock("key", TimeUnit.SECONDS, 1L)).isFalse();
        assertThat(Thread.interrupted()).isTrue();
    }

    private ProceedingJoinPoint pointcut(Method method) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.getTarget()).thenReturn(new Example());
        when(joinPoint.proceed()).thenReturn("ok");
        return joinPoint;
    }
}
