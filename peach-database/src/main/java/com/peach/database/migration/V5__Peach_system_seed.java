package com.peach.database.migration;

/**
 * 执行项目已冻结的系统种子基线，沿用现有幂等插入及密码契约。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 22:00
 */
public final class V5__Peach_system_seed extends BaselineSqlMigration {
    public V5__Peach_system_seed() { super("INIT.sql",false); }
    /** 仅空库初始化种子；接管既有库时不恢复管理员已撤销的成员或权限。 */
    @Override public void migrate(org.flywaydb.core.api.migration.Context context) {
        try (var statement=context.getConnection().createStatement();var result=statement.executeQuery("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY WHERE TYPE='BASELINE'")) {
            if(result.next() && result.getInt(1)>0) return;
        } catch(java.sql.SQLException ex) {
            throw new org.flywaydb.core.api.FlywayException("Cannot inspect Peach baseline marker, SQL state="+ex.getSQLState());
        }
        super.migrate(context);
    }
}
