package com.peach.redis.common.tool;

import com.peach.redis.common.RedisConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/19 10:23
 */
@AutoConfiguration
@AutoConfigureAfter(RedisConfig.class)
public class RedisDaoAutoConfigure {

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean(RedisDao.class)
    public RedisDao redisDao() {
        return new RedisDaoImpl();
    }
}
