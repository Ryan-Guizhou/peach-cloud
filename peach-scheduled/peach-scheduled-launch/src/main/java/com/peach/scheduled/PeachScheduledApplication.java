package com.peach.scheduled;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 调度模块相关说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@EnableScheduling
@MapperScan("com.peach.scheduler.dao")
@SpringBootApplication(scanBasePackages = "com.peach.scheduler")
public class PeachScheduledApplication {

    /**
     * 创建相关对象。
     */
    public PeachScheduledApplication() {
    }

    /**
     * 调度模块相关说明。
     *
     * @param args 参数说明
     */
    public static void main(String[] args) {
        SpringApplication.run(PeachScheduledApplication.class, args);
    }
}
