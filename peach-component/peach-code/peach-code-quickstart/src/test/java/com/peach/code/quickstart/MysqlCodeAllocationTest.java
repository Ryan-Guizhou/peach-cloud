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
        generator = new PeachCodeGenerator(jdbc, new CodeProperties(), null, manager);
    }

    @Test
    void shouldAllocateOnlyUsingMysqlRowLockAndConditionalUpdate() {
        when(jdbc.queryForMap(anyString(), eq("T001"), eq("MENU")))
                .thenReturn(Map.of("MAX_CODE_WIDTH", 8, "CURRENT_VALUE", 41L, "STATUS", "ENABLE"));
        when(jdbc.update(anyString(), eq(42L), eq("T001"), eq("MENU"), eq(41L)))
                .thenReturn(1);

        assertThat(generator.next("T001", "menu")).isEqualTo("MENU_00000042");
        verify(jdbc).update(anyString(), eq(42L), eq("T001"), eq("MENU"), eq(41L));
    }

    @Test
    void shouldRejectOverflowWithoutUpdatingSequence() {
        when(jdbc.queryForMap(anyString(), eq("T001"), eq("MENU")))
                .thenReturn(Map.of("MAX_CODE_WIDTH", 1, "CURRENT_VALUE", 9L, "STATUS", "ENABLE"));

        assertThatThrownBy(() -> generator.next("T001", "MENU"))
                .isInstanceOf(CodeGeneratorException.class)
                .hasMessageContaining("MAX_CODE_WIDTH");
        org.mockito.Mockito.verifyNoMoreInteractions(jdbc);
    }
}
