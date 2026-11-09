package com.peach.mybatis.sql;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据库产品名匹配。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
class DatabaseIdsTest {

    @Test
    void shouldMatchKnownProducts() {
        var mappings = DatabaseIds.defaults();
        assertThat(DatabaseIds.match("MySQL", mappings)).isEqualTo("mysql");
        assertThat(DatabaseIds.match("DM DBMS", mappings)).isEqualTo("mysql");
        assertThat(DatabaseIds.match("PostgreSQL", mappings)).isEqualTo("postgresql");
        assertThat(DatabaseIds.match("MariaDB", mappings)).isEqualTo("mysql");
        assertThat(DatabaseIds.match("Microsoft SQL Server", mappings)).isEqualTo("sqlserver");
        assertThat(DatabaseIds.match("KingbaseES", mappings)).isEqualTo("postgresql");
        assertThat(DatabaseIds.match("UnknownDB", mappings)).isNull();
    }
}
