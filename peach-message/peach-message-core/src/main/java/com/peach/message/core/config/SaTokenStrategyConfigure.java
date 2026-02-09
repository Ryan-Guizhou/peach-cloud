package com.peach.message.core.config;

import com.peach.message.sasession.SaSessionForJacksonCustomized;
import cn.dev33.satoken.strategy.SaStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

import javax.annotation.PostConstruct;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/4 18:09
 */
@Slf4j
@Indexed
@Configuration
public class SaTokenStrategyConfigure {

    @PostConstruct
    public void rewriteSaSessionStrategy(){
        SaStrategy.instance.createSession = SaSessionForJacksonCustomized::new;
    }
}
