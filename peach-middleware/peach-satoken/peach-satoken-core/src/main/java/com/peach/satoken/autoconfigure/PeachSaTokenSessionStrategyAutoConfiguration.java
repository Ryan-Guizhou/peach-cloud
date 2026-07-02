package com.peach.satoken.autoconfigure;

import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.satoken.config.PeachSaTokenProperties;
import com.peach.satoken.session.PeachSaSessionForJacksonCustomized;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import javax.annotation.PostConstruct;

@AutoConfiguration
@ConditionalOnClass(SaStrategy.class)
@ConditionalOnProperty(prefix = "peach.satoken.session-strategy", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PeachSaTokenProperties.class)
public class PeachSaTokenSessionStrategyAutoConfiguration {

    @PostConstruct
    public void rewriteSaSessionStrategy() {
        SaStrategy.instance.createSession = PeachSaSessionForJacksonCustomized::new;
    }
}
