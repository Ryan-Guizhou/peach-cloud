package com.peach.gateway.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 网关实时 IAM 授权服务配置。
 *
 * <p>默认禁用远程验证器，未部署 Auth 受信接口时受保护 API 默认拒绝。
 * 只有 #209 受信服务已联调、权限目录已正确注册，才可启用。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:55
 */
@Data
@ConfigurationProperties(prefix = "peach.gateway.iam")
public class GatewayIamProperties {

    /** 启用对 Auth 受信接口的在线授权核验，默认关闭。 */
    private boolean enabled = false;

    /** 远程鉴权请求总超时，超时一律拒绝，不使用旧缓存兜底。 */
    private Duration timeout = Duration.ofMillis(1500);
}
