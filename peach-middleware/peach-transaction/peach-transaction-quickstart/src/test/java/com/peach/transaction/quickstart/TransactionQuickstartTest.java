package com.peach.transaction.quickstart;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 离线启动烟雾测试，禁止误认作跨库事务回滚验收。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
class TransactionQuickstartTest {

    @Test
    void contextStartsWithoutSeataInfrastructureWhenDisabled() {
        SpringApplication app = new SpringApplication(TransactionQuickstartApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        try (ConfigurableApplicationContext context = app.run("--seata.enabled=false",
                "--peach.transaction.enabled=false")) {
            assertNotNull(context);
        }
    }
}
