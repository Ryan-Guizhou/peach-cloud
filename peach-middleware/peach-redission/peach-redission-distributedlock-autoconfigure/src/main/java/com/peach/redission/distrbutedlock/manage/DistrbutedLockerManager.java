package com.peach.redission.distrbutedlock.manage;

import com.peach.redission.distrbutedlock.locker.DistributedLocker;
import com.peach.redission.distrbutedlock.locker.LockType;
import com.peach.redission.distrbutedlock.locker.RedissionFairLocker;
import com.peach.redission.distrbutedlock.locker.RedissionReadLocker;
import com.peach.redission.distrbutedlock.locker.RedissionRenntrantLocker;
import com.peach.redission.distrbutedlock.locker.RedissionWriteLocker;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.peach.redission.distrbutedlock.locker.LockType.FAIR;
import static com.peach.redission.distrbutedlock.locker.LockType.READ;
import static com.peach.redission.distrbutedlock.locker.LockType.REENTRANT;
import static com.peach.redission.distrbutedlock.locker.LockType.WRITE;

/**
 * DistrbutedLocker管理器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/25 19:02
 */
public class DistrbutedLockerManager {

    public static final Map<LockType, DistributedLocker> LOCKER_CACHE = new ConcurrentHashMap<>();

    public DistrbutedLockerManager(RedissonClient redissonClient){
        LOCKER_CACHE.put(FAIR, new RedissionFairLocker(redissonClient));
        LOCKER_CACHE.put(READ, new RedissionReadLocker(redissonClient));
        LOCKER_CACHE.put(WRITE, new RedissionWriteLocker(redissonClient));
        LOCKER_CACHE.put(REENTRANT, new RedissionRenntrantLocker(redissonClient));
    }

    public DistributedLocker getReentrantLocker(){
        return LOCKER_CACHE.get(REENTRANT);
    }

    public DistributedLocker getFairLocker(){
        return LOCKER_CACHE.get(FAIR);
    }

    public DistributedLocker getWriteLocker(){
        return LOCKER_CACHE.get(WRITE);
    }

    public DistributedLocker getReadLocker(){
        return LOCKER_CACHE.get(READ);
    }
}
