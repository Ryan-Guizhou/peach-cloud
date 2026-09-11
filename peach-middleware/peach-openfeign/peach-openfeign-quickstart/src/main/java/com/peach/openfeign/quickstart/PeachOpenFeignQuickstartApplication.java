package com.peach.openfeign.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Peach OpenFeign Quickstart 启动入口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@SpringBootApplication
@EnableFeignClients
public class PeachOpenFeignQuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachOpenFeignQuickstartApplication.class, args);
    }
}
