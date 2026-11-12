package com.peach.rocket.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Peach RocketMQ Quickstart 启动入口。
 *
 * <p>默认连接配置的 NameServer / Broker；使用 {@code in-process} profile 可运行离线演示。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@SpringBootApplication
public class PeachRocketQuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachRocketQuickstartApplication.class, args);
    }
}
