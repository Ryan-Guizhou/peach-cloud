package com.peach.redis.tool.autoconfigure;

import com.peach.redis.tool.RedisDao;
import com.peach.redis.tool.RedisDaoImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Indexed;

/**
 * 在 Redis 底座之后装配默认 RedisDao，并绑定工具扫描配置。
 *
 * <p>仅在应用存在 RedisTemplate 且未提供 RedisDao 时创建默认实现。
 * 使用名为 redisTemplate 的统一模板，不创建或关闭额外连接工厂。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/19 10:23
 */
@Slf4j
@Indexed
@AutoConfiguration
@EnableConfigurationProperties(RedisToolProperties.class)
@AutoConfigureAfter(name = "com.peach.redis.autoconfigure.PeachRedisAutoConfiguration")
public class RedisDaoAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnMissingBean(RedisDao.class)
    public RedisDao redisDao(@Qualifier("redisTemplate") RedisTemplate<?, ?> redisTemplate,
                             RedisToolProperties properties,
                             @Value("${peach.redis.mode:standalone}") String mode) {
        return new RedisDaoImpl(redisTemplate, properties, mode);
    }
}
