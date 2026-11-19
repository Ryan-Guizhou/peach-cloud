package com.peach.code.quickstart;

import com.peach.code.CodeFormat;
import com.peach.code.CodeGeneratorException;
import com.peach.code.autoconfigure.JdbcCodeRuleMaintenance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 编码规则修改必须保持历史发号单调及格式不可变。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 16:30
 */
class CodeRuleMaintenanceTest {

    private JdbcTemplate jdbc;
    private JdbcCodeRuleMaintenance maintenance;

    @BeforeEach
    void setup() {
        jdbc = mock(JdbcTemplate.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        maintenance = new JdbcCodeRuleMaintenance(jdbc, manager);
        when(jdbc.update(startsWith("INSERT INTO PEACH_CODE_RULE_AUDIT"), any(Object[].class)))
                .thenReturn(1);
    }

    @Test
    void shouldRejectWatermarkRollback() {
        stubRule("T001", "ORG", "TENANT", "ENABLE", 12L, 2L);
        assertThatThrownBy(() -> maintenance.advanceWatermark("T001", "ORG", 11L, 2L, "actor-id"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("decrease");
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldRejectFormatChangesAfterIssuingCodes() {
        stubRule("PLATFORM", "USER", "PLATFORM", "ENABLE", 7L, 3L);
        assertThatThrownBy(() -> maintenance.changeFormat(
                "PLATFORM", "USER", new CodeFormat("USER", "-", 8), 3L, "actor-id"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("immutable");
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldAllowFormatChangeBeforeFirstAllocation() {
        stubRule("T001", "ORG", "TENANT", "ENABLE", 0L, 1L);
        when(jdbc.update(anyString(), eq("-"), eq(8), eq("fixed-id"), eq(1L)))
                .thenReturn(1);
        maintenance.changeFormat("T001", "ORG", new CodeFormat("org", "-", 8), 1L, "actor-id");
        verify(jdbc).update(anyString(), eq("-"), eq(8), eq("fixed-id"), eq(1L));
    }

    @Test
    void shouldAdvanceOnlyToAValidHigherWatermark() {
        stubRule("T001", "ORG", "TENANT", "ENABLE", 5L, 1L);
        when(jdbc.update(anyString(), eq(8L), eq("fixed-id"), eq(1L), eq(5L)))
                .thenReturn(1);
        maintenance.advanceWatermark("T001", "ORG", 8L, 1L, "actor-id");
        verify(jdbc).update(anyString(), eq(8L), eq("fixed-id"), eq(1L), eq(5L));
    }

    @Test
    void shouldAdvanceInPermanentTenantBusinessIdNamespace() {
        stubRule("TENANT_00000001", "ORG", "TENANT", "ENABLE", 7L, 2L);
        when(jdbc.update(anyString(), eq(8L), eq("fixed-id"), eq(2L), eq(7L)))
                .thenReturn(1);
        maintenance.advanceWatermark("TENANT_00000001", "ORG", 8L, 2L, "actor-id");
        verify(jdbc).update(anyString(), eq(8L), eq("fixed-id"), eq(2L), eq(7L));
    }

    @Test
    void shouldRejectConcurrentRuleVersionChange() {
        stubRule("T001", "ORG", "TENANT", "ENABLE", 0L, 4L);
        assertThatThrownBy(() -> maintenance.changeStatus("T001", "ORG", false, 3L, "actor-id"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("version");
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldRejectPlatformRuleInTenantScope() {
        stubRule("PLATFORM", "USER", "TENANT", "ENABLE", 0L, 0L);
        assertThatThrownBy(() -> maintenance.changeStatus("PLATFORM", "USER", false, 0L, "actor-id"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("scope");
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    private void stubRule(String namespace, String prefix, String scope,
                          String status, long current, long version) {
        when(jdbc.queryForMap(anyString(), eq(namespace), eq(prefix)))
                .thenReturn(Map.of(
                        "ID", "fixed-id",
                        "CODE_PREFIX", prefix,
                        "STATUS", status,
                        "SCOPE_TYPE", scope,
                        "CODE_SEPARATOR", "_",
                        "MAX_CODE_WIDTH", 8,
                        "CURRENT_VALUE", current,
                        "RULE_VERSION", version));
    }
}
