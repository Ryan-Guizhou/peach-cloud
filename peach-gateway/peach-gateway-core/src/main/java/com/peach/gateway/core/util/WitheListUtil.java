package com.peach.gateway.core.util;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/25 17:12
 * @Description 白名单工具类
 */
@Slf4j
public class WitheListUtil {


    /** Sa-Token 鉴权放行路径前缀 */
    public static final String[] SA_TOKEN_WHITE_LIST = {

            // ===== 认证 & 用户 =====
            "/login",
            "/logout",
            "/register",
            "/getCaptcha",
            "/checkCaptcha",
            "/init",
            // ===== API 文档 =====
            "/doc.html",
            "/swagger-resources",
            "/webjars",
            "/v3/api-docs",
            "/v2/api-docs",
            // ===== Spring Boot 监控 =====
            "/actuator",
            "health",

            // ===== WebSocket / SSE =====
            "/sse",
            "/favicon.ico",
    };

    private WitheListUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 判断请求的路径是否为白名单(即是否需要校验) / Determine whether the requested path is on the whitelist (i.e., whether validation is required)
     * @param path 请求路径 / Request path
     * @return true:白名单路径 / true:Whitelist path
     */
    public static boolean isWitheList(String path) {
        for (String whitePath : SA_TOKEN_WHITE_LIST) {
            if (path.contains(whitePath)) {
                return true;
            }
        }
        return false;
    }


}
