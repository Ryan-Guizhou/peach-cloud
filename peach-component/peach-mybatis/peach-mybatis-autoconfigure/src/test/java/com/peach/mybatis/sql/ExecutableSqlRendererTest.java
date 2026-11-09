package com.peach.mybatis.sql;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 可执行 SQL 渲染。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
class ExecutableSqlRendererTest {

    @Test
    void shouldInlineParameters() {
        String sql = ExecutableSqlRenderer.render(
                "select * from t where id = ? and name = ?",
                List.of(1, "O'Brien"));

        assertThat(sql).isEqualTo("select * from t where id = 1 and name = 'O''Brien'");
        assertThat(sql).doesNotContain("?");
    }

    @Test
    void shouldKeepPlaceholderInsideLiteralAndComment() {
        String sql = ExecutableSqlRenderer.render(
                "select '?', \"?\", ? -- ?\n, ? /* ? */",
                List.of(2, true));

        assertThat(sql).isEqualTo("select '?', \"?\", 2 -- ?\n, true /* ? */");
    }

    @Test
    void shouldRenderNullWhenArgumentMissing() {
        String sql = ExecutableSqlRenderer.render("update t set name = ? where id = ?", List.of("peach"));

        assertThat(sql).isEqualTo("update t set name = 'peach' where id = NULL");
    }
}
