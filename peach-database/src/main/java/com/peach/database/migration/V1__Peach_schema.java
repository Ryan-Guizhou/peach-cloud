package com.peach.database.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.CRC32;

/**
 * 首版直接创建完整表结构和系统种子，不重放历史升级链。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 22:00
 */
public final class V1__Peach_schema extends BaseJavaMigration {
    private static final List<String> RESOURCES=List.of("ALL_TABLE_CREATE.sql","INIT.sql");
    private byte[] source(String resource) {
        try { return new ClassPathResource("db/peach/baseline/"+resource).getContentAsString(StandardCharsets.UTF_8).replace("\r\n","\n").getBytes(StandardCharsets.UTF_8); }
        catch(java.io.IOException failure) { throw new IllegalStateException("Missing initial database resource"); }
    }
    @Override public Integer getChecksum() {
        var crc=new CRC32();for(String resource:RESOURCES) { crc.update(resource.getBytes(StandardCharsets.UTF_8));crc.update(source(resource)); }return (int)crc.getValue();
    }
    @Override public boolean canExecuteInTransaction() { return false; }
    @Override public void migrate(Context context) {
        try { for(String resource:RESOURCES) ScriptUtils.executeSqlScript(context.getConnection(),new EncodedResource(new ByteArrayResource(source(resource)),StandardCharsets.UTF_8)); }
        catch(org.springframework.dao.DataAccessException failure) { throw new org.flywaydb.core.api.FlywayException("Initial database script failed; inspect the empty development schema before retry"); }
    }
}
