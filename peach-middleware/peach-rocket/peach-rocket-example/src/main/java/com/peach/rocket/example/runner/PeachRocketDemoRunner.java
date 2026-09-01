package com.peach.rocket.example.runner;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import com.peach.rocket.example.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * PeachRocketMQ示例运行器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Slf4j
@Indexed
@Component
@ConditionalOnProperty(prefix = "example.rocket.demo", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PeachRocketDemoRunner implements ApplicationRunner {

    private final OrderService orderService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[example-rocket-demo] start publishing demo messages");
        orderService.publishDemoMessages();
    }
}
