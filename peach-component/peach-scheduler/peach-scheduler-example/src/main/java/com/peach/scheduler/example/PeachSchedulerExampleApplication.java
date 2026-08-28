package com.peach.scheduler.example;

import org.springframework.stereotype.Indexed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Peach调度Example启动类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@SpringBootApplication
@Indexed
public class PeachSchedulerExampleApplication {

    /**
     * 创建 Scheduler 示例应用入口。
     */
    public PeachSchedulerExampleApplication() {
        // Intentionally empty.
    }

    /**
     * 启动示例应用。
     *
     * @param args JVM 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PeachSchedulerExampleApplication.class, args);
    }
}
