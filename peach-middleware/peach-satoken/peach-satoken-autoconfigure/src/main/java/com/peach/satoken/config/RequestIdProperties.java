package com.peach.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web 服务请求 ID 配置。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/8/4
 */
@Data
@ConfigurationProperties(prefix = "peach.satoken.request-id")
public class RequestIdProperties {

    private boolean enabled = true;
    private String headerName = "X-Request-Id";
}
