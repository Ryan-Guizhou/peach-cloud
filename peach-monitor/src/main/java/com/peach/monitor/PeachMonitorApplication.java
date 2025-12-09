package com.peach.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
//import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableCaching
@SpringBootApplication
@EnableDiscoveryClient
//@EnableFeignClients
//@EnableAspectJAutoProxy(proxyTargetClass = true)
public class PeachMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachMonitorApplication.class, args);
    }

}
