package com.peach.userservice.service.config;

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
            log.info("Monitor Service SaInterceptor entering path: " + path);

            // 1. 全局校验 Same-Token (必须从网关转发，防止直连)
            // 网关会给所有转发的请求添加 Same-Token，包括登录接口
            try {
                SaSameUtil.checkCurrentRequestToken();
            } catch (Exception e) {
                log.error("Monitor Service SaInterceptor Same-Token Check Failed: " + e.getMessage());
                throw e;
            }
//            // 排除路径（这里手动判断，确保路径匹配无误）
//            if (path.contains("/login") ||
//                    path.contains("/register") ||
//                    path.contains("/getCaptcha") ||
//                    path.contains("/checkCaptcha") ||
//                    path.contains("/init") ||
//                    path.contains("/doc.html") ||
//                    path.contains("/swagger-resources") ||
//                    path.contains("/webjars") ||
//                    path.contains("/v3/api-docs") ||
//                    path.contains("/favicon.ico") ||
//                    path.contains("/.well-known/appspecific/com.chrome.devtools.json")) {
//
//                return;
//            }

//            // 2. 校验 User-Token (除了登录注册等公开接口)
//            // 虽然网关已经校验了一次，但为了安全深度防御，服务内部也可以再次校验
//            SaRouter.match("/**")
//                    .notMatch(
//                        "/user/login",
//                        "/user/register",
//                        "/user/getCaptcha",
//                        "/user/checkCaptcha",
//                        "/doc.html",
//                        "/swagger-resources/**",
//                        "/webjars/**",
//                        "/user/v3/api-docs",
//                        "/favicon.ico", "/.well-known/appspecific/com.chrome.devtools.json"
//                    )
//                    .check(r -> {
//                        System.out.println("User Service SaInterceptor checking login for: " + path);
//                        StpUtil.checkLogin();
//                    });

        })).addPathPatterns("/**");
    }
}
