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
        if (tables.contains("PEACH_FLYWAY_HISTORY") || tables.isEmpty()) return;
        if (!tables.containsAll(Set.of("PEACH_USER","PEACH_TENANT","PEACH_FUNCTION"))) {
            throw new FlywayException("Refusing to baseline an unrecognized or incomplete Peach schema");
        }
        for (var sentinel : new String[][] {{"PEACH_USER","USER_ID"},{"PEACH_TENANT","TENANT_ID"},{"PEACH_FUNCTION","FUNC_CODE"}}) {
            try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND UPPER(TABLE_NAME)=? AND UPPER(COLUMN_NAME)=?")) {
                statement.setString(1,schema); statement.setString(2,sentinel[0]); statement.setString(3,sentinel[1]);
                try (var result = statement.executeQuery()) {
                    if (!result.next() || result.getInt(1) != 1) throw new FlywayException("Peach baseline signature validation failed");
                }
            }
        }
    }
    private static String lockName(String schema) {
        try { return "peach-migrate:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(schema.getBytes(StandardCharsets.UTF_8))).substring(0,48); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable",ex); }
    }
}
