package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import com.peach.redis.common.RedisConfig;
import com.peach.satoken.dao.PeachSaTokenDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Indexed;

/**
 * Sa-Token DAO 自动配置。
 *
 * <p>当项目中存在 Redis 连接工厂且开启 `peach.satoken.dao.enabled` 时，注册基于 Redis 的 `SaTokenDao` 实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Slf4j
@Indexed
@AutoConfiguration
@AutoConfigureAfter(name = {
        "com.peach.redis.common.RedisConfig"
})
@ConditionalOnClass({SaTokenDao.class, JedisConnectionFactory.class})
@ConditionalOnProperty(prefix = "peach.satoken.dao", name = "enabled", havingValue = "true", matchIfMissing = true)
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

    /**
     * Verify Sa-Token Redis persistence at startup.
     *
     * @param saTokenDao Sa-Token DAO implementation
     * @param environment Spring environment
     * @return startup verifier
     */
    @Bean
    public ApplicationRunner peachSaTokenDaoVerifier(SaTokenDao saTokenDao, Environment environment) {
        return args -> log.info("Peach Sa-Token DAO active, dao={}, tokenName={}, redisMode={}, redisHost={}, redisDatabase={}",
                saTokenDao.getClass().getName(),
                SaManager.getConfig().getTokenName(),
                environment.getProperty("peach.redis.mode"),
                environment.getProperty("peach.redis.host"),
                environment.getProperty("peach.redis.database", "0"));
    }
}
