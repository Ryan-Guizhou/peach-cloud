package com.peach.monitor.launch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;


/**
 * Peach监控启动类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 */
@Slf4j
@EnableDiscoveryClient
@ComponentScan("com.peach")
@SpringBootApplication
public class PeachMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachMonitorApplication.class, args);
    }

}
