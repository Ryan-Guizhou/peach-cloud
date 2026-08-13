package com.peach.sample.distributed;

import org.springframework.stereotype.Indexed;
import com.peach.redission.distrbutedlock.annoation.DistrbutedLock;
import com.peach.redission.distrbutedlock.locker.LockType;
import com.peach.redission.repeat.annoation.RepeatLimit;
import org.springframework.stereotype.Component;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 10:27
 */
@Indexed
@Component
public class DistributedService {

    @DistrbutedLock(name = "distributedLock", keys = {"#p0.id","#p0.name"}, lockType = LockType.FAIR)
    public String getDistributedLock(DistributedLockInfo distributedLockInfo){
        return "distributedLock";
    }

    @RepeatLimit(name = "repeatLimit", keys = {"#p0.id","#p0.name"}, durationTime = 1L,message = "重复提交一秒钟只允许提交一次")
    public String getRepeatLimit(DistributedLockInfo distributedLockInfo){
        return "repeatLimit";
    }

}
