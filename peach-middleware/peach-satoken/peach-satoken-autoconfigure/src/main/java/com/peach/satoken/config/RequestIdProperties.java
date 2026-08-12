package com.peach.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Web 服务请求 ID 配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Data
@ConfigurationProperties(prefix = "peach.satoken.request-id")
public class RequestIdProperties {

    private boolean enabled = true;
    private String headerName = "X-Request-Id";
}
