package com.peach.code.autoconfigure;

import com.peach.code.CodeFormat;
import com.peach.code.CodeGeneratorException;
import com.peach.code.CodeRuleMaintenance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.function.Function;

/**
 * 使用规则行锁及乐观版本控制业务编码规则的安全修改。
 *
 * <p>本类不发布 REST 接口，必须由具备可信管理员授权的业务服务调用。
 * 修改与常规发号共用 MySQL 规则行锁，已发出编号后禁止格式变更或水位回退。
 * 失败时抛异常且回滚，不降级至其他存储。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 16:30
 */
public class JdbcCodeRuleMaintenance implements CodeRuleMaintenance {

    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,31}");
    private static final String SELECT_RULE = "SELECT ID, STATUS, SCOPE_TYPE, CODE_PREFIX, "
            + "CODE_SEPARATOR, MAX_CODE_WIDTH, CURRENT_VALUE, RULE_VERSION "
            + "FROM PEACH_CODE_RULE WHERE TENANT_ID = ? AND CODE_PREFIX = ? "
            + "AND IS_DELETE = 0 FOR UPDATE";
    private static final String UPDATE_STATUS = "UPDATE PEACH_CODE_RULE "
            + "SET STATUS = ?, RULE_VERSION = RULE_VERSION + 1 "
            + "WHERE ID = ? AND RULE_VERSION = ? AND CURRENT_VALUE = ? AND IS_DELETE = 0";
    private static final String UPDATE_FORMAT = "UPDATE PEACH_CODE_RULE "
            + "SET CODE_SEPARATOR = ?, MAX_CODE_WIDTH = ?, RULE_VERSION = RULE_VERSION + 1 "
            + "WHERE ID = ? AND RULE_VERSION = ? AND CURRENT_VALUE = 0 AND IS_DELETE = 0";
    private static final String UPDATE_WATERMARK = "UPDATE PEACH_CODE_RULE "
            + "SET CURRENT_VALUE = ?, RULE_VERSION = RULE_VERSION + 1 "
            + "WHERE ID = ? AND RULE_VERSION = ? AND CURRENT_VALUE = ? AND IS_DELETE = 0";
    private static final String INSERT_AUDIT = "INSERT INTO PEACH_CODE_RULE_AUDIT "
            + "(ID, RULE_REF_ID, RULE_VERSION, OPERATOR_REF_ID, OPERATION, "
            + "OLD_STATUS, NEW_STATUS, OLD_SEPARATOR, NEW_SEPARATOR, OLD_WIDTH, NEW_WIDTH, "
            + "OLD_WATERMARK, NEW_WATERMARK, CREATED_AT) VALUES "
            + "(REPLACE(UUID(), '-', ''), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3))";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transaction;

    /**
     * 初始化规则维护入口。
     *
     * @param jdbcTemplate 与发号器使用同一数据库的 JDBC 模板
     * @param manager 唯一 MySQL 事务管理器
     */
    public JdbcCodeRuleMaintenance(JdbcTemplate jdbcTemplate, PlatformTransactionManager manager) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        if (manager == null) {
            throw new CodeGeneratorException("MySQL transaction manager is required for code rule maintenance");
        }
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Override
    public void changeStatus(String namespace, String prefix, boolean enabled, long expectedVersion,
                             String operatorRefId) {
        requireActor(operatorRefId);
        withinRule(namespace, prefix, expectedVersion, rule -> {
            String status = enabled ? "ENABLE" : "DISABLE";
            if (status.equals(rule.status())) {
                return null;
            }
            assertUpdated(jdbcTemplate.update(UPDATE_STATUS,
                    status, rule.id(), rule.version(), rule.current()), "status");
            appendAudit(rule, operatorRefId, "STATUS", status, rule.separator(), rule.width(), rule.current());
            return null;
        });
    }

    @Override
    public void changeFormat(String namespace, String prefix, CodeFormat format, long expectedVersion,
                             String operatorRefId) {
        Objects.requireNonNull(format, "format");
        requireActor(operatorRefId);
        withinRule(namespace, prefix, expectedVersion, rule -> {
            if (!rule.prefix().equals(format.prefix())) {
                throw new CodeGeneratorException("Code format prefix cannot change");
            }
            if (rule.separator().equals(format.separator()) && rule.width() == format.width()) {
                return null;
            }
            if (rule.current() != 0L) {
                throw new CodeGeneratorException("Issued code rule format is immutable");
            }
            assertUpdated(jdbcTemplate.update(UPDATE_FORMAT,
                    format.separator(), format.width(), rule.id(), rule.version()), "format");
            appendAudit(rule, operatorRefId, "FORMAT", rule.status(),
                    format.separator(), format.width(), rule.current());
            return null;
        });
    }

    @Override
    public void advanceWatermark(String namespace, String prefix, long watermark, long expectedVersion,
                                 String operatorRefId) {
        requireActor(operatorRefId);
        withinRule(namespace, prefix, expectedVersion, rule -> {
            if (watermark < rule.current()) {
                throw new CodeGeneratorException("Code watermark cannot decrease");
            }
            if (watermark > maxValue(rule.width())) {
                throw new CodeGeneratorException("Code watermark exceeds configured width");
            }
            if (watermark == rule.current()) {
                return null;
            }
            assertUpdated(jdbcTemplate.update(UPDATE_WATERMARK,
                    watermark, rule.id(), rule.version(), rule.current()), "watermark");
            appendAudit(rule, operatorRefId, "WATERMARK", rule.status(),
                    rule.separator(), rule.width(), watermark);
            return null;
        });
    }

    private void withinRule(String namespace, String prefix, long expectedVersion,
                            Function<Rule, Void> action) {
        if (expectedVersion < 0) {
            throw new CodeGeneratorException("Invalid expected rule version");
        }
        String safeNamespace = safePart(namespace, "namespace");
        String safePrefix = safePart(prefix, "prefix").toUpperCase(Locale.ENGLISH);
        transaction.execute(status -> {
            Rule rule = loadRule(safeNamespace, safePrefix);
            if (rule.version() != expectedVersion) {
                throw new CodeGeneratorException("Code rule version conflict");
            }
            return action.apply(rule);
        });
    }

    private Rule loadRule(String namespace, String prefix) {
        Map<String, Object> row = jdbcTemplate.queryForMap(SELECT_RULE, namespace, prefix);
        String scope = "PLATFORM".equals(namespace) ? "PLATFORM" : "TENANT";
        if (!scope.equals(row.get("SCOPE_TYPE"))) {
            throw new CodeGeneratorException("Code rule scope does not match namespace");
        }
        Object version = row.get("RULE_VERSION");
        Object current = row.get("CURRENT_VALUE");
        Object width = row.get("MAX_CODE_WIDTH");
        if (!(version instanceof Number) || !(current instanceof Number) || !(width instanceof Number)) {
            throw new CodeGeneratorException("Code rule contains invalid numeric fields");
        }
        long versionNumber = ((Number) version).longValue();
        long currentNumber = ((Number) current).longValue();
        int widthNumber = ((Number) width).intValue();
        if (versionNumber < 0 || currentNumber < 0 || widthNumber < 1 || widthNumber > 18
                || currentNumber > maxValue(widthNumber)) {
            throw new CodeGeneratorException("Code rule contains invalid counters");
        }
        String separator = Objects.toString(row.get("CODE_SEPARATOR"), "");
        if (!"_".equals(separator) && !"-".equals(separator)) {
            throw new CodeGeneratorException("Code rule separator is invalid");
        }
        return new Rule(Objects.toString(row.get("ID"), ""),
                prefix, Objects.toString(row.get("STATUS"), ""),
                separator, widthNumber, currentNumber, versionNumber);
    }

    /**
     * 与规则修改置于相同数据库事务，审计写入失败时拒绝提交管理变更。
     *
     * @param rule 加锁后的原规则
     * @param operatorRefId 可信操作人物理 ID
     * @param operation 变更类型
     * @param nextStatus 变更后的状态
     * @param nextSeparator 变更后的分隔符
     * @param nextWidth 变更后的数字宽度
     * @param nextWatermark 变更后的水位
     */
    private void appendAudit(Rule rule, String operatorRefId, String operation,
                             String nextStatus, String nextSeparator, int nextWidth, long nextWatermark) {
        int inserted = jdbcTemplate.update(INSERT_AUDIT,
                rule.id(), rule.version() + 1, operatorRefId, operation,
                rule.status(), nextStatus, rule.separator(), nextSeparator,
                rule.width(), nextWidth, rule.current(), nextWatermark);
        if (inserted != 1) {
            throw new CodeGeneratorException("Failed to persist code rule change audit");
        }
    }

    private static void requireActor(String operatorRefId) {
        if (!StringUtils.hasText(operatorRefId) || operatorRefId.length() > 32) {
            throw new CodeGeneratorException("Trusted operator reference ID is required");
        }
    }

    private static long maxValue(int width) {
        long result = 1L;
        for (int index = 0; index < width; index++) {
            result *= 10;
        }
        return result - 1;
    }

    private static String safePart(String value, String name) {
        if (value == null || !SAFE_KEY.matcher(value).matches()) {
            throw new CodeGeneratorException(name + " contains unsupported characters");
        }
        return value;
    }

    private static void assertUpdated(int updated, String operation) {
        if (updated != 1) {
            throw new CodeGeneratorException("Concurrent code rule " + operation + " modification");
        }
    }

    private record Rule(String id, String prefix, String status, String separator,
                        int width, long current, long version) {
    }
}
