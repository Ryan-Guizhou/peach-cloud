package com.peach.auth.openfeign;

import cn.dev33.satoken.same.SaSameUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 * @Description Feign 配置：添加 RequestInterceptor 以透传 Sa-Token
 */
@Slf4j
@Indexed
@Configuration
public class FeignConfigure {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 注入 Same-Token，确保内部服务调用通过鉴权
                template.header(SaSameUtil.SAME_TOKEN, SaSameUtil.getToken());
            }
        };
    }
}
