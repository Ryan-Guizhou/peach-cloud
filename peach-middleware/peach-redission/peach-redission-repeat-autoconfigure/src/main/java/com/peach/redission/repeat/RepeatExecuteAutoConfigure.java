package com.peach.redission.repeat;

import com.peach.redission.common.LocalCacheLock;
import com.peach.redission.common.LockInfoHandle;
import com.peach.redission.common.LockInfoHandleFactory;
import com.peach.redission.common.LockInfoType;
import com.peach.redission.common.RedissionDataHandle;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerFactory;
import com.peach.redission.repeat.aspect.RepeatExecuteLimitAspect;
import com.peach.redission.repeat.lockinfo.impl.RepeatExecuteLimitHandle;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/26 11:36
 */
@AutoConfiguration
public class RepeatExecuteAutoConfigure {

    @Bean(LockInfoType.REPEAT_EXCUTED)
    public LockInfoHandle repeatExecuteLimitHandle(){
        return new RepeatExecuteLimitHandle();
    }

    @Bean
    @ConditionalOnBean({LockInfoHandleFactory.class,RedissionDataHandle.class,LocalCacheLock.class})
    @ConditionalOnMissingBean(RepeatExecuteLimitAspect.class)
    public RepeatExecuteLimitAspect repeatExecuteLimitAspect(LockInfoHandleFactory lockInfoHandleFactory,
                                                             RedissionDataHandle redissionDataHandle,
                                                             LocalCacheLock localCacheLock,
                                                             DistrbutedLockerFactory distrbutedLockerFactory){
        return new RepeatExecuteLimitAspect(lockInfoHandleFactory,redissionDataHandle,localCacheLock,distrbutedLockerFactory);
    }
}
