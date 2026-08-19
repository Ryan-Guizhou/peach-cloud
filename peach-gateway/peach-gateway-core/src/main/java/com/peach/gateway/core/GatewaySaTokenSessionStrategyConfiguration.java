package com.peach.gateway.core;

import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.gateway.core.session.PeachSaSessionForJacksonCustomized;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

import jakarta.annotation.PostConstruct;

/**
 * 网关侧 Sa-Token Session 策略配置。
 *
 * <p>该配置属于网关服务内部模块，由 {@code peach-gateway-launch} 的组件扫描加载。
 * 它将 Sa-Token Session 创建策略替换为网关本地的 Jackson 兼容类型，使网关读取
 * Redis 中的 Sa-Token Session 时与业务服务侧保持序列化契约一致。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/11 14:45
 */
@Indexed
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SaStrategy.class)
@ConditionalOnProperty(prefix = "peach.satoken.session-strategy", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class GatewaySaTokenSessionStrategyConfiguration {

    /**
     * 覆盖 Sa-Token Session 创建策略。
     */
    @PostConstruct
    public void rewriteSaSessionStrategy() {
        SaStrategy.instance.createSession = PeachSaSessionForJacksonCustomized::new;
    }
}
