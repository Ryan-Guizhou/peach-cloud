package com.peach.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Current-user cache configuration.
 */
@Data
@ConfigurationProperties(prefix = "peach.satoken.user-context")
public class UserContextProperties {

    private boolean enabled = true;

    private List<String> publicPaths = Arrays.asList(
            "/error", "/health", "/actuator/**", "/v3/api-docs/**", "/v2/api-docs/**",
            "/auth/login", "/auth/logout", "/auth/register", "/auth/forget", "/auth/reset",
            "/auth/getCaptcha", "/auth/checkCaptcha", "/auth/init"
    );

}
