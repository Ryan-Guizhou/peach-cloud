package com.peach.mybatis.sql;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证参数日志摘要不包含可识别的参数值且输出数量有界。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class SqlParameterLogSafetyTest {

    @Test
    void shouldExposeTypesWithoutCredentials() {
        String summary = SqlParameters.typeSummary(List.of("very-secret-token", 100));
        assertThat(summary).contains("count=2", "String", "Integer");
        assertThat(summary).doesNotContain("very-secret-token", "100");
    }

    @Test
    void shouldLimitDisplayedParameterTypes() {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            values.add("secret-" + i);
        }
        String summary = SqlParameters.typeSummary(values);
        assertThat(summary).contains("count=1000", "...");
        assertThat(summary.length()).isLessThan(200);
        assertThat(summary).doesNotContain("secret-");
    }
}
