package com.peach.mybatis.sql;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据库产品名到 MyBatis databaseId 的映射。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
public final class DatabaseIds {

    private DatabaseIds() {
    }

    /**
     * 默认厂商映射。达梦沿用 mysql 语句，人大金仓沿用 postgresql 语句。
     *
     * @return 有序映射
     */
    public static Map<String, String> defaults() {
        Map<String, String> mappings = new LinkedHashMap<>();
        mappings.put("Microsoft SQL Server", "sqlserver");
        mappings.put("KingbaseES", "postgresql");
        mappings.put("PostgreSQL", "postgresql");
        mappings.put("MariaDB", "mysql");
        mappings.put("MySQL", "mysql");
        mappings.put("Oracle", "oracle");
        mappings.put("DM", "mysql");
        mappings.put("H2", "h2");
        return mappings;
    }

    /**
     * 用产品名包含关系匹配 databaseId，先匹配更长的厂商名。
     *
     * @param productName 驱动返回的产品名
     * @param mappings 厂商映射
     * @return 命中的 databaseId，未命中时返回 null
     */
    public static String match(String productName, Map<String, String> mappings) {
        if (productName == null || productName.isBlank() || mappings == null || mappings.isEmpty()) {
            return null;
        }
        String matched = null;
        int matchedLength = -1;
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String vendor = entry.getKey();
            if (vendor == null || vendor.isBlank() || entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (productName.contains(vendor) && vendor.length() > matchedLength) {
                matched = entry.getValue();
                matchedLength = vendor.length();
            }
        }
        return matched;
    }
}
