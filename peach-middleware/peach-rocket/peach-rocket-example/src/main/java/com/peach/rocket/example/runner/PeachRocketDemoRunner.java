package com.peach.rocket.example.runner;

import org.springframework.stereotype.Indexed;
import com.peach.rocket.example.service.OrderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Peach RocketMQ 示例消息发送 Runner。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "example.rocket.demo", name = "enabled", havingValue = "true")
public class PeachRocketDemoRunner implements ApplicationRunner {

    @Resource
    private OrderService orderService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[example-rocket-demo] start publishing demo messages");
        orderService.publishDemoMessages();
    }
}
