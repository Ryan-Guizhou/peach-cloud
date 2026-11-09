package com.peach.mybatis.sql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 危险 SQL 判断。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
class SqlTextsTest {

    @Test
    void shouldDetectWriteWithoutWhere() {
        assertThat(SqlTexts.missingWhere("update t set name = 'where'")).isTrue();
        assertThat(SqlTexts.missingWhere("delete from t")).isTrue();
        assertThat(SqlTexts.missingWhere("delete from t where id = 1")).isFalse();
        assertThat(SqlTexts.missingWhere("select * from t")).isFalse();
    }
}
