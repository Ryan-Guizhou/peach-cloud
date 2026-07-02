package com.peach.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "peach.satoken")
public class PeachSaTokenProperties {

    private Store store = new Store();

    private SessionStrategy sessionStrategy = new SessionStrategy();

    private SameToken sameToken = new SameToken();

    private Gateway gateway = new Gateway();

    @Data
    public static class Store {

        private boolean enabled = true;
    }

    @Data
    public static class SessionStrategy {

        private boolean enabled = true;
    }

    @Data
    public static class SameToken {

        private boolean enabled = true;

        private boolean logPath = true;

        private List<String> excludePathPatterns = Arrays.asList(
                "/error"
        );
    }

    @Data
    public static class Gateway {

        private boolean enabled = true;

        private boolean injectSameToken = true;

        private boolean tokenStrategyEnabled = true;

        private boolean logPath = true;

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
