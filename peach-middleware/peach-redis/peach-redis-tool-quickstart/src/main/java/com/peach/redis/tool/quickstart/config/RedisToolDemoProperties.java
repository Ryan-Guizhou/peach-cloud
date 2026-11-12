package com.peach.redis.tool.quickstart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

/**
 * 示例运行配置；指定相同运行标识可重复演示，但禁止并行复用同一标识。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 16:00
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "quickstart.redis-tool.demo")
public class RedisToolDemoProperties {
    private boolean enabled = true;
    private boolean cleanupAfterRun = false;
    private String runId = UUID.randomUUID().toString();
}
