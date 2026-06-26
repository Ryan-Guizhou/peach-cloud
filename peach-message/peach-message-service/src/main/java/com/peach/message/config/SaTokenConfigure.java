package com.peach.message.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.same.SaSameUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Indexed;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/24 15:17
 * @Description 内部 Token 校验逻辑：只验证 Same-Token，确保请求来自网关或内部服务
 */
@Slf4j
@Indexed
@Configuration
public class SaTokenConfigure {

    @Bean
    public SaReactorFilter messageSaReactorFilter() {
        return new SaReactorFilter()
                .addInclude("/**")
                .setAuth(obj -> {
                    String path = SaHolder.getRequest().getRequestPath();
                    if (isWhiteList(path)) {
                        return;
                    }
                    log.info("Message Service SaReactorFilter entering path: {}", path);
                    try {
                        SaSameUtil.checkCurrentRequestToken();
                    } catch (Exception e) {
                        log.error("Message Service Same-Token Check Failed, path={}, msg={}", path, e.getMessage());
                        throw e;
                    }
                });
    }

    private boolean isWhiteList(String path) {
        return StringUtils.contains(path, "health")
                || StringUtils.contains(path, "/actuator")
                || StringUtils.contains(path, "/doc.html")
                || StringUtils.contains(path, "/webjars")
                || StringUtils.contains(path, "/swagger-resources")
                || StringUtils.contains(path, "/v3/api-docs")
                || StringUtils.contains(path, "/v2/api-docs")
                // WebSocket 握手只校验业务 token，不强制 Same-Token
                || StringUtils.startsWith(path, "/ws/")
                || StringUtils.startsWith(path, "/webSocket");
    }
}
