package com.peach.redission.distrbutedlock;

import com.peach.redis.common.RedisConfig;
import com.peach.redission.common.LockInfoHandle;
import com.peach.redission.common.LockInfoHandleFactory;
import com.peach.redission.common.LockInfoType;
import com.peach.redission.distrbutedlock.aspect.DistrbutedLockAspect;
import com.peach.redission.distrbutedlock.lockinfo.impl.DistributedLockInfoHandle;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerFactory;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerManager;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/26 9:30
 */
@AutoConfiguration
@AutoConfigureAfter(RedisConfig.class)
public class DistributedLockAutoConfigure {


    @Bean(LockInfoType.DISTRIBUTE)
    public LockInfoHandle lockInfoHandle(){
        return new DistributedLockInfoHandle();
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    @ConditionalOnMissingBean(DistrbutedLockerManager.class)
    public DistrbutedLockerManager distrbutedLockerManager(RedissonClient redissonClient){
        return new DistrbutedLockerManager(redissonClient);
    }

    @Bean
    @ConditionalOnBean(DistrbutedLockerManager.class)
    @ConditionalOnMissingBean(DistrbutedLockerFactory.class)
    public DistrbutedLockerFactory distrbutedLockFactory(DistrbutedLockerManager distrbutedLockerManager){
        return new DistrbutedLockerFactory(distrbutedLockerManager);
    }

    @Bean
    @ConditionalOnBean(DistrbutedLockerManager.class)
    @ConditionalOnMissingBean(DistrbutedLockAspect.class)
    public DistrbutedLockAspect distrbutedLockAspect(LockInfoHandleFactory lockInfoHandleFactory, DistrbutedLockerFactory distrbutedLockFactory){
        return new DistrbutedLockAspect(lockInfoHandleFactory,distrbutedLockFactory);
    }


}
