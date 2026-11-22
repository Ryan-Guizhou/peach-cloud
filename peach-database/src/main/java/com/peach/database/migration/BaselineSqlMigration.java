package com.peach.database.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.flywaydb.core.api.FlywayException;
import org.springframework.dao.DataAccessException;

/**
 * 复用冻结的部署基线 SQL，校验和绑定文件内容；后续变更必须新增版本迁移。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 22:00
 */
abstract class BaselineSqlMigration extends BaseJavaMigration {
    private final String resource;
    private final boolean createMissingOnly;
    BaselineSqlMigration(String resource, boolean createMissingOnly) { this.resource=resource; this.createMissingOnly=createMissingOnly; }
    private byte[] source() {
        try { return new ClassPathResource("db/peach/baseline/"+resource).getContentAsString(StandardCharsets.UTF_8).replace("\r\n","\n").getBytes(StandardCharsets.UTF_8); }
        catch (IOException ex) { throw new IllegalStateException("Missing Peach baseline resource",ex); }
    }
    @Override public Integer getChecksum() { CRC32 crc=new CRC32();crc.update(source());return (int)crc.getValue(); }
    @Override public boolean canExecuteInTransaction() { return false; }
    @Override public void migrate(Context context) {
        String sql=new String(source(),StandardCharsets.UTF_8);
        if (createMissingOnly) sql=missingTables(sql,context.getConnection());
        // Fail-fast execution; do not continue after errors or print full SQL ourselves.
        try {
            ScriptUtils.executeSqlScript(context.getConnection(),new EncodedResource(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)),StandardCharsets.UTF_8));
        } catch (DataAccessException ex) {
            throw new FlywayException("Peach baseline script failed; inspect schema compatibility before retry, errorType="+ex.getClass().getSimpleName());
        }
    }
    private static String missingTables(String sql,Connection connection) {
        var matches=new ArrayList<java.util.regex.MatchResult>();
        Pattern.compile("(?im)^CREATE TABLE (?:IF NOT EXISTS )?`?([A-Z0-9_]+)`?").matcher(sql).results().forEach(matches::add);
        StringBuilder missing=new StringBuilder();
        try(var statement=connection.prepareStatement("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND UPPER(TABLE_NAME)=?")) {
            for(int index=0;index<matches.size();index++) {
                var match=matches.get(index);statement.setString(1,match.group(1).toUpperCase(java.util.Locale.ROOT));
                try(var result=statement.executeQuery()) {
                    if(result.next() && result.getInt(1)==0) missing.append(sql,match.start(),index+1<matches.size()?matches.get(index+1).start():sql.length()).append('\n');
                }
            }
        } catch(SQLException ex) { throw new FlywayException("Peach baseline table inspection failed, SQL state="+ex.getSQLState()); }
        return missing.toString();
    }
}
