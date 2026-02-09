package com.peach.gateway.core.config;

import com.peach.gateway.core.session.SaSessionForJacksonCustomized;
import cn.dev33.satoken.strategy.SaStrategy;
import com.peach.gateway.core.GatewayConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * 1. 重写 Sa-Token 策略
     * 默认的 token 生成策略是：
     * 1. 登录ID + 登录类型 + 随机数
     * 2. 默认的 sha256 加密
     * 3. 默认的 token 长度是 32
     * 4. 默认的 token 生成时间是 1 天
     * 5. 默认的 token 续命时间是 30 分钟
     * 6. 默认的 token 删除时间是 7 天
     * 7. 默认的 token 存储位置是 Redis
     */
    @PostConstruct
    public void rewriteSaTokenStrategy() {
        log.info("Gateway Sa-Token init success");
        SaStrategy.instance.createToken = (loginId, loginType) -> {
            String signKey;
            int random = ThreadLocalRandom.current().nextInt(0,9999);
            try {
                String userId = String.valueOf(loginId);
                signKey = MessageFormat.format(GatewayConstant.TOKEN_INCLUDE_USER,System.currentTimeMillis(),random,userId);
            }catch (Exception e){
                log.error("createToken error:｛｝",e);
                signKey = MessageFormat.format(GatewayConstant.TOKEN_NOT_INCLUDE_USER,System.currentTimeMillis(),random);
            }
            return DigestUtils.sha256Hex(signKey);
        };
    }
}