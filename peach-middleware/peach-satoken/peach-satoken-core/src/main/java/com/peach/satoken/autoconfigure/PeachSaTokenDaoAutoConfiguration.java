package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.dao.SaTokenDao;
import com.peach.satoken.config.PeachSaTokenProperties;
import com.peach.satoken.dao.PeachSaTokenDao;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.lang.NonNull;

/**
 * Sa-Token DAO 自动配置。
 *
 * <p>当项目中存在 Redis 连接工厂且开启 `peach.satoken.dao.enabled` 时，注册基于 Redis 的 `SaTokenDao` 实现。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@AutoConfiguration
@AutoConfigureAfter(name = {
        "com.peach.redis.common.RedisConfig"
})
@ConditionalOnClass({SaTokenDao.class, JedisConnectionFactory.class})
@ConditionalOnProperty(prefix = "peach.satoken.dao", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachSaTokenProperties.class)
public class PeachSaTokenDaoAutoConfiguration {

    /**
     * 创建 Sa-Token DAO。
     *
     * @param jedisConnectionFactory Redis 连接工厂
     * @return Sa-Token DAO 实现
     */
    @Bean
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao saTokenDao(@NonNull JedisConnectionFactory jedisConnectionFactory) {
        return new PeachSaTokenDao(jedisConnectionFactory);
    }
}
