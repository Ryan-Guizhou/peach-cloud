package com.peach.auth.service.support;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录审计保留期与可信代理配置；未配置信任关系时不采信转发IP。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "peach.auth.login-audit")
public class LoginAuditProperties {
    @Min(1) @Max(3650)
    private int retentionDays = 90;
    private List<String> trustedProxies = new ArrayList<>();
}
