package com.peach.satoken.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sa-Token 历史请求 ID 配置。
 *
 * <p>请求 ID 已迁移到 {@code peach.observability.request-id}。该类型仅为已有调用方保留
 * 源码和二进制兼容，不再由 Sa-Token 自动配置加载。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Data
@Deprecated(forRemoval = false, since = "1.0.0")
@ConfigurationProperties(prefix = "peach.satoken.request-id")
public class RequestIdProperties {

    private boolean enabled = true;
    private String headerName = "X-Request-Id";
}
