package com.peach.redis.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 验证 Jedis Redis-only 客户端独立装配的启动入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@SpringBootApplication
public class PeachRedisQuickstartApplication {
    public static void main(String[] args) {
        SpringApplication.run(PeachRedisQuickstartApplication.class, args);
    }
}
