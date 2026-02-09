package com.peach.setting.config;

import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.setting.sasession.SaSessionForJacksonCustomized;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

import javax.annotation.PostConstruct;


@Slf4j
@Indexed
@Configuration
public class SaTokenStrategyConfigure {

    /**
     * 重写session策略
     */
    @PostConstruct
    public void rewriteSaSessionStrategy(){
        SaStrategy.instance.createSession = SaSessionForJacksonCustomized::new;
    }

}