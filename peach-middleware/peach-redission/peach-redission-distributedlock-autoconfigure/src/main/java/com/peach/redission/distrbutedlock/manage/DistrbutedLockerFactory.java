package com.peach.redission.distrbutedlock.manage;


import com.peach.redission.distrbutedlock.locker.DistributedLocker;
import com.peach.redission.distrbutedlock.locker.LockType;
import lombok.extern.slf4j.Slf4j;

/**
 * DistrbutedLocker工厂。
 *
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

    public DistributedLocker getDistrbutedLocker(LockType lockType) {
        return switch (lockType) {
            case FAIR -> distrbutedLockerManager.getFairLocker();
            case READ -> distrbutedLockerManager.getReadLocker();
            case REENTRANT -> distrbutedLockerManager.getReentrantLocker();
            case WRITE -> distrbutedLockerManager.getWriteLocker();
            default -> {
                log.error("lockType is error,lockType:{}", lockType);
                yield null;
            }
        };
    }
}
