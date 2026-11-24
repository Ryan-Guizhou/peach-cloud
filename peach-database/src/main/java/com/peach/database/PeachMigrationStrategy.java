package com.peach.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 串行校验与迁移共享 MySQL；连接持有命名锁到整个启动迁移完成，防止并发首次接管竞态。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 22:00
 */
public final class PeachMigrationStrategy implements FlywayMigrationStrategy {
    /** 失败向上传播并阻止应用启动；不记录连接凭据，也不尝试 clean/repair。 */
    @Override
    public void migrate(Flyway flyway) {
        if (flyway.getConfiguration().getDataSource() instanceof com.zaxxer.hikari.HikariDataSource pool
                && pool.getMaximumPoolSize() > 0 && pool.getMaximumPoolSize() < 3) {
            throw new FlywayException("Peach startup migration requires a Hikari maximum pool size of at least 3");
        }
        try (Connection connection = flyway.getConfiguration().getDataSource().getConnection()) {
            String schema = connection.getCatalog();
            if (schema == null || schema.isBlank()) throw new FlywayException("A selected Peach database is required");
            String lockName = lockName(schema);
            boolean locked = false;
            try {
                try (var statement = connection.prepareStatement("SELECT GET_LOCK(?,120)")) {
                    statement.setString(1,lockName);
                    try (var result = statement.executeQuery()) {
                        locked = result.next() && result.getInt(1) == 1;
                    }
                }
                if (!locked) throw new FlywayException("Timed out waiting for Peach database migration lock");
                validateExistingSchema(connection, schema);
                flyway.migrate();
            } finally {
                if (locked) try (var statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    statement.setString(1,lockName); statement.execute();
                }
            }
        } catch (SQLException ex) {
            // JDBC exceptions may contain URLs or SQL values; keep only a safe category.
            throw new FlywayException("Peach database migration connection/lock failed, SQL state=" + ex.getSQLState());
        }
    }
    private static void validateExistingSchema(Connection connection, String schema) throws SQLException {
        Set<String> tables = new HashSet<>();
        try (var statement = connection.prepareStatement("SELECT UPPER(TABLE_NAME) FROM information_schema.TABLES WHERE TABLE_SCHEMA=?")) {
            statement.setString(1,schema);
            try (var result = statement.executeQuery()) { while (result.next()) tables.add(result.getString(1)); }
        }
        if (tables.isEmpty()) return;
        if (tables.contains("PEACH_FLYWAY_HISTORY")) {
            try(var statement=connection.createStatement();var rows=statement.executeQuery("SELECT COUNT(*) FROM PEACH_FLYWAY_HISTORY WHERE TYPE='BASELINE' OR (SUCCESS=1 AND VERSION IS NOT NULL AND VERSION<>'1')")) {
                rows.next();
                if(rows.getInt(1)>0) throw new FlywayException("Legacy development migration history is incompatible with the consolidated first version; use a new schema");
            }
            return;
        }
        throw new FlywayException("Initial database version requires an empty schema; existing development databases must use a new schema");
    }
    private static String lockName(String schema) {
        try { return "peach-migrate:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(schema.getBytes(StandardCharsets.UTF_8))).substring(0,48); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable",ex); }
    }
}
