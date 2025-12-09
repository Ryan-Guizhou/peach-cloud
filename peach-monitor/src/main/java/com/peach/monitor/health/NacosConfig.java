package com.peach.monitor.health;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 18:04
 */
@Slf4j
@Data
@Component
@RefreshScope
public class NacosConfig {

//    @Value("${company1:1}")
//    private String company1;
//
//
//    @Value("${company3}")
//    private String company3;
//
//    @PostConstruct
//    public void init() {
//        log.info("=== Nacos配置注入结果 ===");
//        log.info("company1 = {}", company1);
//        log.info("company3 = {}", company3);
//        log.info("=== 配置注入完成 ===");
//    }
}
