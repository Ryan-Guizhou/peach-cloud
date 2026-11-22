package com.peach.database.migration;

/**
 * 执行项目已冻结的表结构基线，只补缺表。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 22:00
 */
public final class V1__Peach_schema extends BaselineSqlMigration {
    public V1__Peach_schema() { super("ALL_TABLE_CREATE.sql",true); }
}
