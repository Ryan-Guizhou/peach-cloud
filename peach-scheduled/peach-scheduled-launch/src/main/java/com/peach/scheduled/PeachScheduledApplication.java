package com.peach.scheduled;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PeachScheduled启动类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@EnableScheduling
@MapperScan("com.peach.scheduler.dao")
@SpringBootApplication(scanBasePackages = "com.peach.scheduler")
public class PeachScheduledApplication {


    public static void main(String[] args) {
        SpringApplication.run(PeachScheduledApplication.class, args);
    }
}
