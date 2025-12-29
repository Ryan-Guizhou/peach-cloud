package com.peach.redission.repeat.aspect;


import com.peach.redission.common.LocalCacheLock;
import com.peach.redission.common.LockInfoHandle;
import com.peach.redission.common.LockInfoHandleFactory;
import com.peach.redission.common.LockInfoType;
import com.peach.redission.common.RedissionDataHandle;
import com.peach.redission.distrbutedlock.locker.DistributedLocker;
import com.peach.redission.distrbutedlock.locker.LockType;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerFactory;
import com.peach.redission.repeat.annoation.RepeatLimit;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 11:07
 */
@Slf4j
@Aspect
public class RepeatExecuteLimitAspect {

    public static final String PREFIX_NAME = "repeat_flag";

    public static final String SUCCESS_FLAG = "success";

    private final LocalCacheLock localCacheLock;

    private final RedissionDataHandle redissionDataHandle;

    private final LockInfoHandleFactory lockInfoHandleFactory;

    private final DistrbutedLockerFactory distrbutedLockerFactory;

    public RepeatExecuteLimitAspect(LockInfoHandleFactory lockInfoHandleFactory, RedissionDataHandle redissionDataHandle, LocalCacheLock localCacheLock, DistrbutedLockerFactory distrbutedLockerFactory) {
        this.lockInfoHandleFactory = lockInfoHandleFactory;
        this.redissionDataHandle = redissionDataHandle;
        this.localCacheLock = localCacheLock;
        this.distrbutedLockerFactory = distrbutedLockerFactory;
    }


    @Around("@annotation(repeatLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RepeatLimit repeatLimit) throws Throwable {

        long durationTime = repeatLimit.durationTime();
        String message = repeatLimit.message();
        Object obj;

        LockInfoHandle lockHandle = lockInfoHandleFactory.getLockHandle(LockInfoType.REPEAT_EXCUTED);
        String lockName = lockHandle.getLockName(joinPoint, repeatLimit.name(), repeatLimit.keys());
        String repeatFlagName = PREFIX_NAME + lockName;
        String flagObject = redissionDataHandle.get(repeatFlagName);
        if (SUCCESS_FLAG.equals(flagObject)){
            throw new RuntimeException(message);
        }

        // 获取本地锁
        ReentrantLock localLock = localCacheLock.getLock(lockName, true);
        boolean lockFlag = localLock.tryLock();
        if (!lockFlag){
            throw new RuntimeException(message);
        }

        // 分布式锁
        try {
            DistributedLocker distrbutedLocker = distrbutedLockerFactory.getDistrbutedLocker(LockType.FAIR);
            boolean result = distrbutedLocker.tryLock(lockName, TimeUnit.SECONDS, 0);
            if (result){
                try {
                    flagObject = redissionDataHandle.get(repeatFlagName);
                    if (SUCCESS_FLAG.equals(flagObject)) {
                        throw new RuntimeException(message);
                    }
                    obj = joinPoint.proceed();
                    if (durationTime > 0) {
                        try {
                            redissionDataHandle.set(repeatFlagName,SUCCESS_FLAG,durationTime,TimeUnit.SECONDS);
                        }catch (Exception e) {
                            log.error("getBucket error",e);
                        }
                    }
                    return obj;
                }finally {
                    distrbutedLocker.unlock(lockName);
                }
            }else {
                throw new RuntimeException(message);
            }
        }finally {
            localLock.unlock();
        }
    }
}
