package com.peach.auth.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description 内部 Token 校验逻辑：只验证 Same-Token，确保请求来自网关或内部服务
 */
@Slf4j
@Indexed
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，定义在注册拦截器时里
        registry.addInterceptor(new SaInterceptor(handler -> {
            String path = SaHolder.getRequest().getRequestPath();
            log.info("User Service SaInterceptor entering path: " + path);

            // 1. 全局校验 Same-Token (必须从网关转发，防止直连)
            // 网关会给所有转发的请求添加 Same-Token，包括登录接口
            try {
                SaSameUtil.checkCurrentRequestToken();
            } catch (Exception e) {
                log.error("User Service SaInterceptor Same-Token Check Failed: " + e.getMessage());
                throw e;
            }
        })).addPathPatterns("/**");
    }
}
