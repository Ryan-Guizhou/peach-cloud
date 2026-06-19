package com.peach.setting.config;

import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.setting.sasession.SaSessionForJacksonCustomized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

import javax.annotation.PostConstruct;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description Sa-Token 策略配置
 */
@Slf4j
@Indexed
@Configuration
public class SaTokenStrategyConfigure {

    /**
     * 重写 Sa-Token Session 创建策略。
     */
    @PostConstruct
    public void rewriteSaSessionStrategy() {
        SaStrategy.instance.createSession = SaSessionForJacksonCustomized::new;
    }
}

