package com.peach.gateway.launch;

import com.peach.gateway.core.config.GatewayRiskControlProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/8 16:20
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
@EnableConfigurationProperties(GatewayRiskControlProperties.class)
@ComponentScan("com.peach")
public class PeachGatewayApplication {
    public static void main(String[] args) {
        log.info("gateway starting...");
        SpringApplication.run(PeachGatewayApplication.class, args);
        log.info("gateway started");
    }
}
