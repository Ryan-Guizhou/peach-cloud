package com.peach.common.lock;

import com.peach.common.lock.manager.DistributedLockManager;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 15:07
 */
public class Demo {


    public static void main(String[] args) {
        DistributedLock defaultLock = DistributedLockManager.getDefaultLock("lock:demo");
        boolean isLcok = defaultLock.tryLock();
        if (isLcok){
            System.out.println("获取锁成功");
            try {
                System.out.println(1);
                System.out.println(defaultLock.getName());
                System.out.println("defaultLock = " + defaultLock.getLockInfo());
            }finally {
                defaultLock.unlock();
            }
        }

    }
}
