package com.peach.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Peach Sa-Token extension properties.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Data
@ConfigurationProperties(prefix = "peach.satoken")
public class PeachSaTokenProperties {

    /**
     * Sa-Token DAO properties.
     */
    private Dao dao = new Dao();

    /**
     * Sa-Token session strategy properties.
     */
    private SessionStrategy sessionStrategy = new SessionStrategy();

    /**
     * Sa-Token token creation strategy properties.
     */
    private TokenStrategy tokenStrategy = new TokenStrategy();

    /**
     * Same-Token check properties.
     */
    private SameToken sameToken = new SameToken();

    @Data
    public static class Dao {

        /**
         * Whether to enable custom Sa-Token DAO.
         */
        private boolean enabled = true;
    }

    @Data
    public static class SessionStrategy {

        /**
         * Whether to override Sa-Token session creation strategy.
         */
        private boolean enabled = true;
    }

    @Data
    public static class TokenStrategy {

        /**
         * Whether to override Sa-Token token creation strategy.
         */
        private boolean enabled = true;
    }

    @Data
    public static class SameToken {

        /**
         * Whether to enable Same-Token verification.
         */
        private boolean enabled = true;

        /**
         * Whether to log request paths during Same-Token verification.
         */
        private boolean logPath = true;

        /**
         * Paths excluded from Same-Token verification.
         */
        private List<String> excludePathPatterns = Arrays.asList(
                "/error"
        );
    }

}
