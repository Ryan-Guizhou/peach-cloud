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

@AutoConfiguration
@AutoConfigureAfter(name = {
        "com.peach.redis.common.RedisConfig"
})
@ConditionalOnClass({SaTokenDao.class, JedisConnectionFactory.class})
@ConditionalOnProperty(prefix = "peach.satoken.dao", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachSaTokenProperties.class)
public class PeachSaTokenDaoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao saTokenDao(@NonNull JedisConnectionFactory jedisConnectionFactory) {
        return new PeachSaTokenDao(jedisConnectionFactory);
    }
}
