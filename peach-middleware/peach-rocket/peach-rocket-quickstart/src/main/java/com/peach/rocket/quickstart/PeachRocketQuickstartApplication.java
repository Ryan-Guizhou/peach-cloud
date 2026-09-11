package com.peach.rocket.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Peach RocketMQ Quickstart 启动入口。
 *
 * <p>默认排除 RocketMQ Spring 自动配置，由进程内 {@code MqPublisher} 完成发布-消费闭环，
 * 不依赖外部 NameServer / Broker。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@SpringBootApplication(excludeName = "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration")
public class PeachRocketQuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(PeachRocketQuickstartApplication.class, args);
    }
}
