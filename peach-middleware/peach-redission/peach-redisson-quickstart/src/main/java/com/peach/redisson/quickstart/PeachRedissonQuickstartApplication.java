package com.peach.redisson.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Redisson 独立客户端启动样例，不依赖 Spring Data Redis 或 Jedis。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/10
 */
@SpringBootApplication
public class PeachRedissonQuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachRedissonQuickstartApplication.class, args);
    }
}
