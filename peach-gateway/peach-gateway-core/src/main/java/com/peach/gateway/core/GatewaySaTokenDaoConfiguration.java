package com.peach.gateway.core;

import cn.dev33.satoken.dao.SaTokenDao;
import com.peach.gateway.core.dao.PeachSaTokenDao;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Indexed;

/**
 * 网关侧 Sa-Token Redis DAO 配置。
 *
 * <p>该配置属于网关服务内部模块，由 {@code peach-gateway-launch} 的组件扫描加载。
 * 网关不引入业务服务侧 {@code peach-satoken}，但需要使用同一套 Redis Sa-Token 数据。
 * 存在 Redis 连接工厂且 {@code peach.satoken.dao.enabled=true} 时，注册网关独立的
 * {@link PeachSaTokenDao}。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Indexed
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(name = {
        "com.peach.redis.common.RedisConfig"
})
@ConditionalOnClass({SaTokenDao.class, JedisConnectionFactory.class})
@ConditionalOnBean(JedisConnectionFactory.class)
@ConditionalOnProperty(prefix = "peach.satoken.dao", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewaySaTokenDaoConfiguration {

    /**
     * 创建网关侧 Sa-Token DAO。
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
