package com.peach.redission.distrbutedlock.manage;

import com.peach.redission.distrbutedlock.locker.DistributedLocker;
import com.peach.redission.distrbutedlock.locker.LockType;
import com.peach.redission.distrbutedlock.locker.RedissionFairLocker;
import com.peach.redission.distrbutedlock.locker.RedissionReadLocker;
import com.peach.redission.distrbutedlock.locker.RedissionRenntrantLocker;
import com.peach.redission.distrbutedlock.locker.RedissionWriteLocker;
import org.redisson.api.RedissonClient;

import java.util.Map;

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

    private final Map<LockType, DistributedLocker> lockerCache;

    public DistrbutedLockerManager(RedissonClient redissonClient){
        this.lockerCache = Map.of(
                FAIR, new RedissionFairLocker(redissonClient),
                READ, new RedissionReadLocker(redissonClient),
                WRITE, new RedissionWriteLocker(redissonClient),
                REENTRANT, new RedissionRenntrantLocker(redissonClient));
    }

    public DistributedLocker getReentrantLocker(){
        return lockerCache.get(REENTRANT);
    }

    public DistributedLocker getFairLocker(){
        return lockerCache.get(FAIR);
    }

    public DistributedLocker getWriteLocker(){
        return lockerCache.get(WRITE);
    }

    public DistributedLocker getReadLocker(){
        return lockerCache.get(READ);
    }
}
