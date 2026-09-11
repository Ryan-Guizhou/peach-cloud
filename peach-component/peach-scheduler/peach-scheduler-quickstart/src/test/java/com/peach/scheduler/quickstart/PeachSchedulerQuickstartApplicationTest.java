package com.peach.scheduler.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 Scheduler QuickStart 的最小启动入口声明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 09:00
 */
class PeachSchedulerQuickstartApplicationTest {

    @Test
    void shouldDeclareSpringBootApplication() {
        assertThat(PeachSchedulerQuickstartApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }
}
