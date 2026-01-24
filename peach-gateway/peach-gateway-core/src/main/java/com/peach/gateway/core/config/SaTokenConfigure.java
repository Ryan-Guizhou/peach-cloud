package com.peach.gateway.core.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.same.SaSameUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Indexed;

import java.util.Arrays;
import java.util.List;

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
                    if (path.contains("/login") ||
                        path.contains("/register") ||
                        path.contains("/getCaptcha") ||
                        path.contains("/checkCaptcha") ||
                        path.contains("/init") ||
                        path.contains("/doc.html") ||
                        path.contains("/swagger-resources") ||
                        path.contains("/webjars") ||
                        path.contains("/v3/api-docs") ||
                        path.contains("/favicon.ico") ||
                        path.contains("/.well-known/appspecific/com.chrome.devtools.json")) {
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
