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
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import java.lang.reflect.Method;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Indexed;

import java.util.concurrent.TimeUnit;

/**
 * 防重放切面。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:29
 * @Description 防重放切面
 */
@Slf4j
@Indexed
@Aspect
public class RepeatExecuteLimitAspect {

    public static final String PREFIX_NAME = "repeat_flag";

    public static final String SUCCESS_FLAG = "success";

    private final RedissionDataHandle redissionDataHandle;

    private final LockInfoHandleFactory lockInfoHandleFactory;

    private final DistrbutedLockerFactory distrbutedLockerFactory;

    public RepeatExecuteLimitAspect(LockInfoHandleFactory lockInfoHandleFactory, RedissionDataHandle redissionDataHandle, LocalCacheLock localCacheLock, DistrbutedLockerFactory distrbutedLockerFactory) {
        this.lockInfoHandleFactory = lockInfoHandleFactory;
        this.redissionDataHandle = redissionDataHandle;
        this.distrbutedLockerFactory = distrbutedLockerFactory;
    }


    /**
     * 方法级防重优先于类级防重，类注解作用于经 Spring AOP 代理的方法。
     */
    @Around("@annotation(com.peach.redission.repeat.annoation.RepeatLimit) || "
            + "@within(com.peach.redission.repeat.annoation.RepeatLimit)")
    public Object aroundAnnotated(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        RepeatLimit annotation = AnnotatedElementUtils.findMergedAnnotation(targetMethod, RepeatLimit.class);
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(method, RepeatLimit.class);
        }
        if (annotation == null) {
            annotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, RepeatLimit.class);
        }
        if (annotation == null) {
            return joinPoint.proceed();
        }
        return around(joinPoint, annotation);
    }

    /** 执行已解析的防重策略，保留原有入口供兼容测试使用。 */
    public Object around(ProceedingJoinPoint joinPoint, RepeatLimit repeatLimit) throws Throwable {

        long durationTime = repeatLimit.durationTime();
        String message = repeatLimit.message();
        LockInfoHandle lockHandle = lockInfoHandleFactory.getLockHandle(LockInfoType.REPEAT_EXECUTED);
        String lockName = lockHandle.getLockName(joinPoint, repeatLimit.name(), repeatLimit.keys());
        String repeatFlagName = PREFIX_NAME + lockName;
        String flagObject = redissionDataHandle.get(repeatFlagName);
        if (SUCCESS_FLAG.equals(flagObject)){
            throw new IllegalStateException(message);
        }

        // 只使用跨实例分布式锁；本地锁会增加锁排序与锁对象生命周期风险。
        DistributedLocker locker = distrbutedLockerFactory.getDistrbutedLocker(LockType.FAIR);
        boolean acquired = locker.tryLock(lockName, TimeUnit.SECONDS, 0);
        if (!acquired) {
            throw new IllegalStateException(message);
        }
        try {
            String savedFlag = redissionDataHandle.get(repeatFlagName);
            if (SUCCESS_FLAG.equals(savedFlag)) {
                throw new IllegalStateException(message);
            }
            Object result = joinPoint.proceed();
            if (durationTime > 0) {
                try {
                    redissionDataHandle.set(repeatFlagName, SUCCESS_FLAG, durationTime, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    // The business operation may already have succeeded. Do not silently return success.
                    log.error("Repeat guard persistence failed after business execution, key={}", repeatFlagName, exception);
                    throw new IllegalStateException(
                            "Business action executed, but repeat guard state is unknown; do not retry blindly",
                            exception);
                }
            }
            return result;
        } finally {
            locker.unlock(lockName);
        }
    }
}
