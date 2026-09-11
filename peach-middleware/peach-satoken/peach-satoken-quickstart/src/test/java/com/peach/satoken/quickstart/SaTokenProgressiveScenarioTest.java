package com.peach.satoken.quickstart;

import com.peach.satoken.quickstart.scenario.ProgressiveDemo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 SaToken 由易到难全级别演示可执行（Demo 内使用 SaTokenContextMockUtil）。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:30
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quickstart.satoken.demo.enabled=false")
class SaTokenProgressiveScenarioTest {

    @Autowired
    private List<ProgressiveDemo> demos;

    @Test
    void shouldExposeProgressiveDemosAcrossThreeLevels() {
        assertThat(demos).hasSizeGreaterThanOrEqualTo(3);
        assertThat(demos.stream().map(ProgressiveDemo::level).distinct().count()).isEqualTo(3);
    }

    @Test
    void shouldExecuteAllProgressiveDemos() {
        demos.stream()
                .sorted(Comparator.comparingInt((ProgressiveDemo d) -> d.level().order())
                        .thenComparingInt(ProgressiveDemo::order))
                .forEach(ProgressiveDemo::execute);
    }
}
