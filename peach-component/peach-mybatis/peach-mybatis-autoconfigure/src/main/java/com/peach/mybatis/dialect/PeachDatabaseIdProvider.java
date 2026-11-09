package com.peach.mybatis.dialect;

import com.peach.mybatis.sql.DatabaseIds;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.mapping.DatabaseIdProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * 按驱动返回的数据库产品名选择 MyBatis {@code databaseId}。
 * <p>匹配规则是产品名包含配置中的厂商名，较长的厂商名优先，因此 {@code DM DBMS} 可以命中 {@code DM}。
 * 没有任何映射命中时抛出异常，避免 {@code databaseId} 为空后带 {@code databaseId} 的语句被静默跳过。
 * 映射来自 {@code peach.mybatis.database-ids}，会覆盖同名默认值。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@Slf4j
public class PeachDatabaseIdProvider implements DatabaseIdProvider {

    private final Map<String, String> mappings;

    public PeachDatabaseIdProvider(Map<String, String> mappings) {
        this.mappings = Map.copyOf(mappings);
    }

    @Override
    public void setProperties(Properties properties) {
        // 映射由 peach.mybatis.database-ids 提供。
    }

    @Override
    public String getDatabaseId(DataSource dataSource) {
        String productName = productName(dataSource);
        String databaseId = DatabaseIds.match(productName, mappings);
        if (databaseId == null) {
            throw new IllegalStateException("Unrecognized database product [" + productName
                    + "]. Add a mapping under peach.mybatis.database-ids");
        }
        log.info("MyBatis database identified: product={}, databaseId={}", productName, databaseId);
        return databaseId;
    }

    private static String productName(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to read database product name", exception);
        }
    }
}
