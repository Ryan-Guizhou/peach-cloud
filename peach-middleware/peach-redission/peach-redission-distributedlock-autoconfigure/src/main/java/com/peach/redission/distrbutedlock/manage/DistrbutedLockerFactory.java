package com.peach.redission.distrbutedlock.manage;


import com.peach.redission.distrbutedlock.locker.DistributedLocker;
import com.peach.redission.distrbutedlock.locker.LockType;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/25 19:00
 */
@Slf4j
public class DistrbutedLockerFactory {

    private final DistrbutedLockerManager distrbutedLockerManager;

    public DistrbutedLockerFactory(DistrbutedLockerManager distrbutedLockerManager){
        this.distrbutedLockerManager = distrbutedLockerManager;
    }

    public DistributedLocker getDistrbutedLocker(LockType lockType){
        switch (lockType){
            case FAIR:
                return distrbutedLockerManager.getFairLocker();
            case READ:
                return distrbutedLockerManager.getReadLocker();
            case REENTRANT:
                return distrbutedLockerManager.getReentrantLocker();
            case WRITE:
                return distrbutedLockerManager.getWriteLocker();
            default:
                log.error("lockType is error,lockType:{}",lockType);
                return null;
        }
    }
}
