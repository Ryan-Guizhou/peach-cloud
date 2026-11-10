package com.peach.redission.repeat.aspect;

import com.peach.redission.common.LockInfoHandle;
import com.peach.redission.common.LockInfoHandleFactory;
import com.peach.redission.common.LockInfoType;
import com.peach.redission.common.LocalCacheLock;
import com.peach.redission.common.RedissionDataHandle;
import com.peach.redission.distrbutedlock.locker.DistributedLocker;
import com.peach.redission.distrbutedlock.locker.LockType;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerFactory;
import com.peach.redission.repeat.annoation.RepeatLimit;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 防重复成功标记保存失败的状态可见性测试。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
class RepeatExecuteLimitAspectTest {

    @Test
    void markerPersistenceFailureAfterBusinessSuccessMustBeVisible() throws Throwable {
        LockInfoHandleFactory infoFactory = mock(LockInfoHandleFactory.class);
        LockInfoHandle info = mock(LockInfoHandle.class);
        RedissionDataHandle data = mock(RedissionDataHandle.class);
        DistrbutedLockerFactory lockerFactory = mock(DistrbutedLockerFactory.class);
        DistributedLocker locker = mock(DistributedLocker.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        RepeatLimit repeatLimit = mock(RepeatLimit.class);

        when(infoFactory.getLockHandle(LockInfoType.REPEAT_EXECUTED)).thenReturn(info);
        when(info.getLockName(eq(joinPoint), anyString(), any(String[].class))).thenReturn("order:1");
        when(repeatLimit.name()).thenReturn("order");
        when(repeatLimit.keys()).thenReturn(new String[]{"id"});
        when(repeatLimit.message()).thenReturn("Duplicate request");
        when(repeatLimit.durationTime()).thenReturn(30L);
        when(lockerFactory.getDistrbutedLocker(LockType.FAIR)).thenReturn(locker);
        when(locker.tryLock("order:1", TimeUnit.SECONDS, 0L)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("accepted");
        doThrow(new IllegalStateException("redis unavailable"))
                .when(data).set("repeat_flagorder:1", "success", 30L, TimeUnit.SECONDS);

        RepeatExecuteLimitAspect aspect = new RepeatExecuteLimitAspect(
                infoFactory, data, new LocalCacheLock(), lockerFactory);

        assertThatThrownBy(() -> aspect.around(joinPoint, repeatLimit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Business action executed")
                .hasCauseInstanceOf(IllegalStateException.class);
        verify(joinPoint).proceed();
        verify(locker).unlock("order:1");
    }
}
