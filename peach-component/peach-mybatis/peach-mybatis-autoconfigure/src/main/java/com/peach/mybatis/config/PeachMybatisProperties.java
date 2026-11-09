package com.peach.mybatis.config;

import com.peach.mybatis.sql.DatabaseIds;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * peach.mybatis 配置。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@Data
@ConfigurationProperties(prefix = "peach.mybatis")
public class PeachMybatisProperties {

    /**
     * 是否启用多库识别、SQL 日志和慢 SQL。
     */
    private boolean enabled = true;

    /**
     * 慢 SQL 阈值，单位毫秒。
     */
    private int longQueryTime = 3000;

    /**
     * 为 true 时把 jdbcTypeForNull 设为 NULL，避免 Oracle 插入空值失败。
     */
    private boolean jdbcTypeForNull = false;

    /**
     * 产品名到 databaseId 的映射。配置项会覆盖同名默认值。
     */
    private Map<String, String> databaseIds = new LinkedHashMap<>(DatabaseIds.defaults());

    /**
     * 日志与告警。
     */
    private Log log = new Log();

    /**
     * PageHelper 参数。classpath 存在 PageHelper 时生效。
     */
    private Pagehelper pagehelper = new Pagehelper();

    /**
     * 合并默认映射与外部配置，更长的厂商名优先匹配。
     *
     * @return 最终映射
     */
    public Map<String, String> resolvedDatabaseIds() {
        Map<String, String> resolved = new LinkedHashMap<>(DatabaseIds.defaults());
        if (databaseIds != null) {
            databaseIds.forEach((vendor, databaseId) -> {
                if (vendor != null && !vendor.isBlank() && databaseId != null && !databaseId.isBlank()) {
                    resolved.put(vendor, databaseId);
                }
            });
        }
        return resolved;
    }

    /**
     * SQL 日志格式与告警。
     * <p>{@code executable=false} 时只输出带 {@code ?} 的语句和 Parameters。
     * {@code executable=true} 时只输出参数已替换的可执行语句。两种格式不会同时出现。</p>
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/9 16:40
     */
    @Data
    public static class Log {

        /**
         * 是否输出可执行 SQL。
         * <p>false：Preparing 保留 {@code ?}，并单独输出 Parameters。
         * true：Preparing 为可直接执行的 SQL，不再输出 Parameters，也不再保留 {@code ?} 形式。</p>
         */
        private boolean executable = false;

        /**
         * UPDATE / DELETE 去掉字符串和注释后仍没有 WHERE 时打告警。告警里的语句遵循 executable 开关。
         */
        private boolean warnMissingWhere = true;

        /**
         * DEBUG 日志中 Preparing 文本的最大长度。慢 SQL 和缺少 WHERE 的告警不截断，Parameters 行不截断。
         * 小于等于 0 表示不截断。
         */
        private int maxSqlLength = 4000;

        /**
         * 单次查询返回行数超过该值时告警。小于等于 0 表示关闭。
         */
        private int largeResultThreshold = 5000;
    }

    /**
     * PageHelper 行为。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/9 16:40
     */
    @Data
    public static class Pagehelper {

        /**
         * 分页方言。为空时由 PageHelper 按当前连接识别一次。
         */
        private String helperDialect = "";

        /**
         * 为 true 时每次分页都重新识别方言。默认 false，一条连接只识别一次。
         */
        private boolean autoRuntimeDialect = false;

        /**
         * 页码小于 1 时回到第一页，页码超过总页数时回到最后一页。
         */
        private boolean reasonable = true;

        /**
         * 为 true 时从 Mapper 方法参数中读取 pageNum、pageSize。默认关闭，分页参数走 PageHelper.startPage。
         */
        private boolean supportMethodsArguments = false;

        /**
         * 分页结束后是否关闭连接。默认 false，连接交还给连接池。
         */
        private boolean closeConn = false;
    }
}
