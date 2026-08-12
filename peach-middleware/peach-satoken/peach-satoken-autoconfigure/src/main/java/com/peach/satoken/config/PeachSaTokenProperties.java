package com.peach.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Peach Sa-Token 配置属性。
 *
 * <p>该配置用于统一管理 Sa-Token 的 DAO、会话策略和 Same-Token 校验。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Data
@ConfigurationProperties(prefix = "peach.satoken")
public class PeachSaTokenProperties {

    /**
     * DAO 相关配置。
     */
    private Dao dao = new Dao();

    /**
     * 会话策略相关配置。
     */
    private SessionStrategy sessionStrategy = new SessionStrategy();

    /**
     * 同 token 校验相关配置。
     */
    private SameToken sameToken = new SameToken();

    @Data
    public static class Dao {

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

}
