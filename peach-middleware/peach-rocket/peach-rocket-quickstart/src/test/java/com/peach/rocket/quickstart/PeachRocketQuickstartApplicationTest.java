package com.peach.rocket.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 RocketMQ QuickStart 的最小启动入口声明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 09:00
 */
class PeachRocketQuickstartApplicationTest {

    @Test
    void shouldDeclareSpringBootApplication() {
        assertThat(PeachRocketQuickstartApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }
}
