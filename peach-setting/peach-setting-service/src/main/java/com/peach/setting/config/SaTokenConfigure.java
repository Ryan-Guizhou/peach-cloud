package com.peach.setting.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.same.SaSameUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description Sa-Token 拦截器配置
 */
@Slf4j
@Indexed
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
            String path = SaHolder.getRequest().getRequestPath();
            log.info("Setting Service SaInterceptor entering path: {}", path);
            try {
                SaSameUtil.checkCurrentRequestToken();
            } catch (Exception e) {
                log.error("Setting Service SaInterceptor Same-Token Check Failed: {}", e.getMessage());
                throw e;
            }
        })).addPathPatterns("/**");
    }
}

