package com.peach.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Peach Sa-Token 配置属性。
 *
 * <p>该配置用于统一管理 Sa-Token 的 DAO、会话策略、同 token 校验以及网关侧相关开关与白名单。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Data
@ConfigurationProperties(prefix = "peach.satoken")
public class PeachSaTokenProperties {

    /**
     * DAO 相关配置。
     */
    private Store store = new Store();

    /**
     * 会话策略相关配置。
     */
    private SessionStrategy sessionStrategy = new SessionStrategy();

    /**
     * 同 token 校验相关配置。
     */
    private SameToken sameToken = new SameToken();

    /**
     * 网关侧 Sa-Token 相关配置。
     */
    private Gateway gateway = new Gateway();

    @Data
    public static class Store {

        /**
         * 是否启用 Sa-Token DAO。
         */
        private boolean enabled = true;
    }

    @Data
    public static class SessionStrategy {

        /**
         * 是否启用会话策略覆盖。
         */
        private boolean enabled = true;
    }

    @Data
    public static class SameToken {

        /**
         * 是否启用同 token 校验。
         */
        private boolean enabled = true;

        /**
         * 是否在日志中打印请求路径。
         */
        private boolean logPath = true;

        /**
         * 同 token 校验排除路径。
         */
        private List<String> excludePathPatterns = Arrays.asList(
                "/error"
        );
    }

    @Data
    public static class Gateway {

        /**
         * 是否启用网关侧 Sa-Token 能力。
         */
        private boolean enabled = true;

        /**
         * 是否在网关请求中注入同 token。
         */
        private boolean injectSameToken = true;

        /**
         * 是否启用网关侧 token 生成策略。
         */
        private boolean tokenStrategyEnabled = true;

        /**
         * 是否在日志中打印请求路径。
         */
        private boolean logPath = true;

        /**
         * 网关白名单路径。
         */
        private List<String> whiteList = Arrays.asList(
                "/login",
                "/logout",
                "/register",
                "/getCaptcha",
                "/checkCaptcha",
                "/init",
                "/doc.html",
                "/swagger-resources",
                "/webjars",
                "/v3/api-docs",
                "/v2/api-docs",
                "/actuator",
                "health",
                "/sse",
                "/favicon.ico"
        );
    }
}
