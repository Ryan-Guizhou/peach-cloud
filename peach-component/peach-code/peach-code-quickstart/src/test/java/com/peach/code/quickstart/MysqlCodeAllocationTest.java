package com.peach.code.quickstart;

import com.peach.code.CodeGeneratorException;
import com.peach.code.autoconfigure.CodeProperties;
import com.peach.code.autoconfigure.PeachCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 MySQL 唯一发号权威的基础事务与溢出语义。
 *
 * <p>这里只验证 SQL 调用和事务入口；真实行锁竞争及断库故障需在 MySQL 集成环境验收。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 11:30
 */
class MysqlCodeAllocationTest {

    private JdbcTemplate jdbc;
    private PeachCodeGenerator generator;

    @BeforeEach
    void setup() {
        jdbc = mock(JdbcTemplate.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        generator = new PeachCodeGenerator(jdbc, new CodeProperties(), manager);
    }

    @Test
    void shouldAllocateOnlyUsingMysqlRowLockAndConditionalUpdate() {
        when(jdbc.queryForMap(anyString(), eq("T001"), eq("MENU")))
                .thenReturn(Map.of("MAX_CODE_WIDTH", 8, "CURRENT_VALUE", 41L, "STATUS", "ENABLE", "SCOPE_TYPE", "TENANT", "RULE_VERSION", 0L));
        when(jdbc.update(anyString(), eq(42L), eq("T001"), eq("MENU"), eq(41L), eq(0L), eq("TENANT")))
                .thenReturn(1);

        assertThat(generator.next("T001", "menu")).isEqualTo("MENU_00000042");
        verify(jdbc).update(anyString(), eq(42L), eq("T001"), eq("MENU"), eq(41L), eq(0L), eq("TENANT"));
    }

    @Test
    void shouldAllocateInPermanentTenantBusinessIdNamespace() {
        when(jdbc.queryForMap(anyString(), eq("TENANT_00000001"), eq("ORG")))
                .thenReturn(Map.of("MAX_CODE_WIDTH", 8, "CURRENT_VALUE", 0L,
                        "STATUS", "ENABLE", "SCOPE_TYPE", "TENANT", "RULE_VERSION", 0L));
        when(jdbc.update(anyString(), eq(1L), eq("TENANT_00000001"), eq("ORG"),
                eq(0L), eq(0L), eq("TENANT"))).thenReturn(1);
        assertThat(generator.next("TENANT_00000001", "ORG")).isEqualTo("ORG_00000001");
    }

    @Test
    void shouldUseConfiguredSeparatorWithoutASecondAllocationPath() {
        when(jdbc.queryForMap(anyString(), eq("T001"), eq("ORG")))
                .thenReturn(Map.of("MAX_CODE_WIDTH", 5, "CURRENT_VALUE", 7L,
                        "STATUS", "ENABLE", "CODE_SEPARATOR", "-", "SCOPE_TYPE", "TENANT", "RULE_VERSION", 0L));
        when(jdbc.update(anyString(), eq(8L), eq("T001"), eq("ORG"), eq(7L), eq(0L), eq("TENANT")))
                .thenReturn(1);

        assertThat(generator.next("T001", "org")).isEqualTo("ORG-00008");
    }

    @Test
    void shouldRejectInvalidSeparatorBeforeUpdatingSequence() {
        when(jdbc.queryForMap(anyString(), eq("T001"), eq("ORG")))
                .thenReturn(Map.of("MAX_CODE_WIDTH", 5, "CURRENT_VALUE", 7L,
                        "STATUS", "ENABLE", "CODE_SEPARATOR", "/", "SCOPE_TYPE", "TENANT", "RULE_VERSION", 0L));
        assertThatThrownBy(() -> generator.next("T001", "ORG"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("separator");
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldRejectScopeMismatchBeforeAllocation() {
        when(jdbc.queryForMap(anyString(), eq("PLATFORM"), eq("USER")))
                .thenReturn(Map.of("MAX_CODE_WIDTH", 8, "CURRENT_VALUE", 0L,
                        "STATUS", "ENABLE", "SCOPE_TYPE", "TENANT", "RULE_VERSION", 0L));
        assertThatThrownBy(() -> generator.nextGlobal("USER"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("scope");
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldRejectInvalidRuleVersionBeforeAllocation() {
        when(jdbc.queryForMap(anyString(), eq("T001"), eq("ORG")))
                .thenReturn(Map.of("MAX_CODE_WIDTH", 8, "CURRENT_VALUE", 0L,
                        "STATUS", "ENABLE", "SCOPE_TYPE", "TENANT", "RULE_VERSION", -1L));
        assertThatThrownBy(() -> generator.next("T001", "ORG"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("version");
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldRejectOverflowWithoutUpdatingSequence() {
        when(jdbc.queryForMap(anyString(), eq("T001"), eq("MENU")))
                .thenReturn(Map.of("MAX_CODE_WIDTH", 1, "CURRENT_VALUE", 9L, "STATUS", "ENABLE", "SCOPE_TYPE", "TENANT", "RULE_VERSION", 0L));

        assertThatThrownBy(() -> generator.next("T001", "MENU"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("MAX_CODE_WIDTH");
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
