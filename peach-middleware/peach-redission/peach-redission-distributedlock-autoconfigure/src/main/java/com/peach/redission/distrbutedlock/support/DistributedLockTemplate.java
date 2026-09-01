package com.peach.redission.distrbutedlock.support;

import com.peach.redission.distrbutedlock.lockinfo.impl.DistributedLockInfoHandle;
import com.peach.redission.distrbutedlock.locker.DistributedLocker;
import com.peach.redission.distrbutedlock.locker.LockType;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 编程式分布式锁模板，与 {@code @DistrbutedLock} 使用同一套锁名规则与 Redisson 实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/1
 */
@Slf4j
@Indexed
@RequiredArgsConstructor
public class DistributedLockTemplate {

    private static final long DEFAULT_WAIT_SECONDS = 10L;

    private final DistrbutedLockerFactory distrbutedLockerFactory;

    /**
     * 在默认可重入锁保护下执行无返回值任务。
     *
     * @param businessName 业务锁名称
     * @param resourceKey  资源键
     * @param action       临界区任务
     */
    public void run(String businessName, String resourceKey, Runnable action) {
        run(businessName, new String[]{resourceKey}, LockType.REENTRANT, DEFAULT_WAIT_SECONDS, TimeUnit.SECONDS, action);
    }

    /**
     * 在指定锁类型保护下执行无返回值任务。
     *
     * @param businessName 业务锁名称
     * @param resourceKey  资源键
     * @param lockType     锁类型
     * @param action       临界区任务
     */
    public void run(String businessName, String resourceKey, LockType lockType, Runnable action) {
        run(businessName, new String[]{resourceKey}, lockType, DEFAULT_WAIT_SECONDS, TimeUnit.SECONDS, action);
    }

    /**
     * 在默认可重入锁保护下执行有返回值任务。
     *
     * @param businessName 业务锁名称
     * @param resourceKey  资源键
     * @param supplier     临界区任务
     * @param <T>          返回类型
     * @return 任务结果
     */
    public <T> T call(String businessName, String resourceKey, Supplier<T> supplier) {
        return call(businessName, new String[]{resourceKey}, LockType.REENTRANT, DEFAULT_WAIT_SECONDS, TimeUnit.SECONDS, supplier);
    }

    /**
     * 在指定锁类型保护下执行有返回值任务。
     *
     * @param businessName 业务锁名称
     * @param resourceKey  资源键
     * @param lockType     锁类型
     * @param supplier     临界区任务
     * @param <T>          返回类型
     * @return 任务结果
     */
    public <T> T call(String businessName, String resourceKey, LockType lockType, Supplier<T> supplier) {
        return call(businessName, new String[]{resourceKey}, lockType, DEFAULT_WAIT_SECONDS, TimeUnit.SECONDS, supplier);
    }

    /**
     * 在分布式锁保护下执行无返回值任务。
     *
     * @param businessName 业务锁名称
     * @param resourceKeys 资源键数组
     * @param lockType     锁类型
     * @param waitTime     最大等待时间
     * @param unit         时间单位
     * @param action       临界区任务
     */
    public void run(String businessName, String[] resourceKeys, LockType lockType,
                    long waitTime, TimeUnit unit, Runnable action) {
        call(businessName, resourceKeys, lockType, waitTime, unit, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 在分布式锁保护下执行有返回值任务。
     *
     * @param businessName 业务锁名称
     * @param resourceKeys 资源键数组
     * @param lockType     锁类型
     * @param waitTime     最大等待时间
     * @param unit         时间单位
     * @param supplier     临界区任务
     * @param <T>          返回类型
     * @return 任务结果
     */
    public <T> T call(String businessName, String[] resourceKeys, LockType lockType,
                      long waitTime, TimeUnit unit, Supplier<T> supplier) {
        String lockName = resolveLockName(businessName, resourceKeys);
        DistributedLocker locker = distrbutedLockerFactory.getDistrbutedLocker(lockType);
        boolean acquired = locker.tryLock(lockName, unit, waitTime);
        if (!acquired) {
            throw new IllegalStateException("Failed to acquire distributed lock: " + businessName);
        }
        try {
            return supplier.get();
        } finally {
            locker.unlock(lockName);
        }
    }

    /**
     * 解析分布式锁名称
     * @param businessName
     * @param resourceKeys
     * @return
     */
    private String resolveLockName(String businessName, String[] resourceKeys) {
        StringBuilder lockName = new StringBuilder();
        lockName.append(DistributedLockInfoHandle.LOCK_PREFIX_NAME)
                .append(":")
                .append(businessName)
                .append(":")
                .append(String.join("-", resourceKeys));
        log.info("Resolved lock name: {}", new String(lockName));
        return new String(lockName);
    }
}
