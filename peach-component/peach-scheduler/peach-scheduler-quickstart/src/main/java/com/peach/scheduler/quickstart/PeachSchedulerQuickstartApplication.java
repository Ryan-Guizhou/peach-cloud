package com.peach.scheduler.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Peach Scheduler Quickstart 启动入口。
 *
 * <p>本地不依赖生产控制面与真实 RocketMQ；Claim / 结果上报使用内存桩，
 * {@link com.peach.scheduler.core.PeachJobExecutor} 由 starter 自动装配。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@SpringBootApplication
public class PeachSchedulerQuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachSchedulerQuickstartApplication.class, args);
    }
}
