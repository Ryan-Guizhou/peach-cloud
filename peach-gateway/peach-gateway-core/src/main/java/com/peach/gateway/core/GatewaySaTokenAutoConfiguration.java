package com.peach.gateway.core;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.dao.SaTokenDao;
import com.peach.gateway.core.dao.PeachSaTokenDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Indexed;

/**
 * 网关侧 Sa-Token Redis 自动配置。
 *
 * <p>必须通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * 注册，并在 {@code RedisConfig} 之后、{@code SaBeanInject} 之前加载。不要在组件扫描中重复注册，
 * 否则 {@code @ConditionalOnBean(JedisConnectionFactory)} 可能过早评估导致整类配置被跳过。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Slf4j
@Indexed
@AutoConfiguration
@AutoConfigureAfter(name = "com.peach.redis.common.RedisConfig")
@AutoConfigureBefore(name = "cn.dev33.satoken.spring.SaBeanInject")
@ConditionalOnClass({SaTokenDao.class, JedisConnectionFactory.class})
@ConditionalOnProperty(prefix = "peach.satoken.dao", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GatewaySaTokenAutoConfiguration {

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

    /**
     * 在 Sa-Token DAO 初始化完成后立即绑定到 {@link SaManager}。
     *
     * @return Bean 后置处理器
     */
    @Bean
    static SaTokenDaoBindingPostProcessor saTokenDaoBindingPostProcessor() {
        return new SaTokenDaoBindingPostProcessor();
    }

    /**
     * 启动时输出 Sa-Token Redis 绑定信息，便于确认网关未退回内存 DAO。
     *
     * @param saTokenDao Sa-Token DAO 实现
     * @param environment Spring 环境
     * @return 启动校验任务
     */
    @Bean
    ApplicationRunner gatewaySaTokenDaoVerifier(SaTokenDao saTokenDao, Environment environment) {
        return args -> log.info(
                "Gateway Peach Sa-Token DAO active, dao={}, managerDao={}, tokenName={}, redisMode={}, redisHost={}, redisDatabase={}",
                saTokenDao.getClass().getName(),
                SaManager.getSaTokenDao().getClass().getName(),
                SaManager.getConfig().getTokenName(),
                environment.getProperty("peach.redis.mode"),
                environment.getProperty("peach.redis.host"),
                environment.getProperty("peach.redis.database", "0"));
    }

    /**
     * 将 Peach Sa-Token DAO 绑定到全局 {@link SaManager}。
     */
    static final class SaTokenDaoBindingPostProcessor implements BeanPostProcessor {

        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
            if (bean instanceof PeachSaTokenDao peachSaTokenDao) {
                SaManager.setSaTokenDao(peachSaTokenDao);
                log.info("Gateway Sa-Token DAO bound to SaManager, beanName={}, dao={}",
                        beanName, peachSaTokenDao.getClass().getName());
            }
            return bean;
        }
    }
}
