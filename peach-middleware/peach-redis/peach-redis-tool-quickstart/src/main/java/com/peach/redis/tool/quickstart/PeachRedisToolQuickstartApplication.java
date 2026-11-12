package com.peach.redis.tool.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.peach.redis.tool.quickstart.config.RedisToolDemoProperties;

/**
 * Peach Redis Tool Quickstart 启动入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/10 18:30
 */
@SpringBootApplication
@EnableConfigurationProperties(RedisToolDemoProperties.class)
public class PeachRedisToolQuickstartApplication {
    public static void main(String[] args) {
        SpringApplication.run(PeachRedisToolQuickstartApplication.class, args);
    }
}
