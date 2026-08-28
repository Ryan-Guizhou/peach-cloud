package com.peach.gateway.launch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;


/**
 * Peach网关启动类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/8 16:20
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
@ComponentScan("com.peach")
public class PeachGatewayApplication {
    public static void main(String[] args) {
        log.info("gateway starting...");
        SpringApplication.run(PeachGatewayApplication.class, args);
        log.info("gateway started");
    }
}
