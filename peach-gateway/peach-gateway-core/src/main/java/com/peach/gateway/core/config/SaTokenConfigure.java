package com.peach.gateway.core.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaStrategy;
import cn.dev33.satoken.util.SaResult;
import com.peach.common.util.StringUtil;
import com.peach.gateway.core.GatewayConstant;
import com.peach.gateway.core.util.WitheListUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Indexed;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description 网关,校验外部token，签发内部token
 */
@Slf4j
@Indexed
@Configuration
public class SaTokenConfigure {


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
                String userId = StringUtil.getStringValue(loginId);
                signKey = MessageFormat.format(GatewayConstant.TOKEN_INCLUDE_USER,System.currentTimeMillis(),random,userId);
            }catch (Exception e){
                log.error("createToken error:｛｝",e);
                signKey = MessageFormat.format(GatewayConstant.TOKEN_NOT_INCLUDE_USER,System.currentTimeMillis(),random);
            }
            return DigestUtils.sha256Hex(signKey);
        };
    }


    /**
     * 1. 注册 Sa-Token 全局过滤器 (Reactor 版)
     * 负责：登录校验、权限认证
     */
    @Bean
    public SaReactorFilter getSaReactorFilter() {
        return new SaReactorFilter()
                // 拦截所有请求
                .addInclude("/**")
                // 鉴权方法
                .setAuth(obj -> {
                    // 获取当前请求路径
                    String path = SaHolder.getRequest().getRequestPath();
                    log.info("Gateway Service SaInterceptor entering path: " + path);
                    if (WitheListUtil.isWitheList(path)) {
                        log.info("Gateway Sa-Token don't need check token, path: {}",path);
                        return;
                    }
                    log.warn("Gateway Sa-Token start check token, path: {}",path);
                    StpUtil.checkLogin();
                })
                // 异常处理方法
                .setError(e -> {
                    log.error("Gateway Sa-Token Authentication failed {}", e.getMessage());
                    return SaResult.error(e.getMessage());
                });
    }

    /**
     * 2. 全局过滤器：注入 Same-Token
     * 负责：在请求转发给下游服务前，注入 Same-Token 头
     * 这样下游服务（配置了 checkCurrentRequestToken）才能通过鉴权
     */
    @Bean
    public GlobalFilter sameTokenFilter() {
        return (exchange, chain) -> {
            // 生成 Same-Token
            String sameToken = SaSameUtil.getToken();
            
            // 注入到请求头
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(SaSameUtil.SAME_TOKEN, sameToken)
                    .build();
            
            // 继续执行链
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

}
