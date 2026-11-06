package com.peach.gateway.core.ratelimit.service;

import com.peach.gateway.core.ratelimit.config.GatewayRateLimitProperties;
import com.peach.gateway.core.ratelimit.script.RateLimitScriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 网关启动及 Nacos 配置刷新时加载限流脚本。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/05
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitScriptLoader implements ApplicationRunner {

    private final GatewayRateLimitProperties properties;
    private final RateLimitScriptRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.info("Gateway rate limit is disabled; Lua script loading is skipped");
            return;
        }
        try {
            repository.reload().block();
            log.info("Gateway rate limit Lua scripts loaded");
        } catch (RuntimeException exception) {
            log.error("Gateway rate limit Lua script loading failed, errorType={}",
                    rootCause(exception).getClass().getSimpleName());
            throw new IllegalStateException(
                    "Unable to load gateway rate limit scripts; verify OSS URL and SHA-256 configuration");
        }
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChanged(EnvironmentChangeEvent event) {
        boolean reloadRequired = event.getKeys().stream().anyMatch(key ->
                key.startsWith("peach.gateway.rate-limit.scripts.")
                        || "peach.gateway.rate-limit.enabled".equals(key));
        boolean transportChanged = event.getKeys().stream().anyMatch(key ->
                "peach.gateway.rate-limit.scripts.connect-timeout".equals(key)
                        || "peach.gateway.rate-limit.scripts.read-timeout".equals(key)
                        || "peach.gateway.rate-limit.scripts.max-script-bytes".equals(key));
        if (transportChanged) {
            repository.invalidateWebClient();
        }
        if (!properties.isEnabled() || !reloadRequired) {
            return;
        }
        repository.reload().subscribe(
                ignored -> log.info("Gateway rate limit Lua scripts reloaded"),
                error -> log.error("Gateway rate limit Lua script reload failed, errorType={}",
                        rootCause(error).getClass().getSimpleName()));
    }

    private Throwable rootCause(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
