package com.peach.redission.repeat;

import com.peach.redission.common.LocalCacheLock;
import com.peach.redission.common.LockInfoHandle;
import com.peach.redission.common.LockInfoHandleFactory;
import com.peach.redission.common.LockInfoType;
import com.peach.redission.common.RedissionDataHandle;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerFactory;
import com.peach.redission.repeat.aspect.RepeatExecuteLimitAspect;
import com.peach.redission.repeat.lockinfo.impl.RepeatExecuteLimitHandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Indexed;

/**
 * 防重复ExecuteAuto自动配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/26 11:36
 */
@Slf4j
@Indexed
@AutoConfiguration
@AutoConfigureAfter(name = {
        "com.peach.redisson.autoconfigure.PeachRedissonAutoConfiguration",
        "com.peach.redission.common.RedissionCommonAutoconfiguration",
        "com.peach.redission.distrbutedlock.DistributedLockAutoConfiguration"
})
public class RepeatExecuteAutoConfiguration {

    @Bean(LockInfoType.REPEAT_EXECUTED)
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
