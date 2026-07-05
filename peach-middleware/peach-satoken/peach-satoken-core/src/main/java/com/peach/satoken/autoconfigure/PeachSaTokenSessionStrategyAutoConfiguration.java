package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.satoken.config.PeachSaTokenProperties;
import com.peach.satoken.session.PeachSaSessionForJacksonCustomized;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import javax.annotation.PostConstruct;

/**
 * Sa-Token 会话策略自动配置。
 *
 * <p>用于替换默认会话创建策略，使 Sa-Token 会话对象使用当前项目定制实现。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@AutoConfiguration
@ConditionalOnClass(SaStrategy.class)
@ConditionalOnProperty(prefix = "peach.satoken.session-strategy", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachSaTokenProperties.class)
public class PeachSaTokenSessionStrategyAutoConfiguration {

    /**
     * 覆盖 Sa-Token 的会话创建策略。
     */
    @PostConstruct
    public void rewriteSaSessionStrategy() {
        SaStrategy.instance.createSession = PeachSaSessionForJacksonCustomized::new;
    }
}
