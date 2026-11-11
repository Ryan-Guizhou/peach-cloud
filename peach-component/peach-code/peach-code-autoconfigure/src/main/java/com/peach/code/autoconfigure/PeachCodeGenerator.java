package com.peach.code.autoconfigure;

import com.peach.code.CodeGenerator;
import com.peach.code.CodeGeneratorException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 使用唯一 MySQL 事实源分配租户业务编码。
 *
 * <p>每次分配都在独立事务内锁定租户与前缀对应的规则行，校验后更新水位。
 * Redis 不参与发号，避免 Redis 失败和 MySQL 降级期间出现两个并发分配者。
 * 分配事务独立于业务写入事务，业务回滚后序号不会回收。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public class PeachCodeGenerator implements CodeGenerator {

    private static final Pattern SAFE_PART = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,31}");
    private static final String RULE_SELECT_SQL = "SELECT MAX_CODE_WIDTH, CURRENT_VALUE, STATUS "
            + "FROM PEACH_CODE_RULE WHERE TENANT_ID = ? AND CODE_PREFIX = ? FOR UPDATE";
    private static final String RULE_UPDATE_SQL = "UPDATE PEACH_CODE_RULE SET CURRENT_VALUE = ?, "
            + "MODIFY_TIME = DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s') "
            + "WHERE TENANT_ID = ? AND CODE_PREFIX = ? AND STATUS = 'ENABLE' AND CURRENT_VALUE = ?";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate allocationTransaction;

    /**
     * 创建发号器。保留原构造签名供已有配置与扩展使用。
     *
     * @param jdbcTemplate 业务数据源 JDBC 模板
     * @param properties 原有配置对象；Redis 开关不再决定发号权威
     * @param redisTemplate 原有可选 Redis 依赖；不再执行 Redis 发号
     * @param transactionManager 必需的 MySQL 事务管理器
     */
    public PeachCodeGenerator(JdbcTemplate jdbcTemplate, CodeProperties properties,
                              StringRedisTemplate redisTemplate,
                              PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = java.util.Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        if (transactionManager == null) {
            throw new CodeGeneratorException("MySQL transaction manager is required for code allocation");
        }
        this.allocationTransaction = new TransactionTemplate(transactionManager);
        this.allocationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.allocationTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    /**
     * 分配下一业务编码。同一租户和前缀必须在表中对应唯一规则行。
     *
     * <p>锁定规则行后校验最大值，溢出时回滚，存储故障时直接失败，不从缓存中继续分配。
     * 事务成功提交但业务调用失败可能产生空号，不会回收序号。</p>
     *
     * @param tenantId 租户标识
     * @param prefix 编码前缀
     * @return 租户隔离的业务编码
     */
    @Override
    public String next(String tenantId, String prefix) {
        validatePart(tenantId, "tenantId");
        validatePart(prefix, "prefix");
        String normalizedPrefix = prefix.toUpperCase(Locale.ENGLISH);
        try {
            String result = allocationTransaction.execute(status -> allocate(tenantId, normalizedPrefix));
            if (result == null) {
                throw new CodeGeneratorException("MySQL code allocation returned null");
            }
            return result;
        } catch (CodeGeneratorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CodeGeneratorException("Failed to allocate business code: " + normalizedPrefix, ex);
        }
    }

    private String allocate(String tenantId, String prefix) {
        Map<String, Object> row = jdbcTemplate.queryForMap(RULE_SELECT_SQL, tenantId, prefix);
        if (!"ENABLE".equalsIgnoreCase(String.valueOf(row.get("STATUS")))) {
            throw new CodeGeneratorException("Code rule is disabled: " + prefix);
        }

        Object widthObject = row.get("MAX_CODE_WIDTH");
        Object currentObject = row.get("CURRENT_VALUE");
        if (!(widthObject instanceof Number) || !(currentObject instanceof Number)) {
            throw new CodeGeneratorException("Invalid code rule values: " + prefix);
        }
        int width = ((Number) widthObject).intValue();
        long current = ((Number) currentObject).longValue();
        if (width < 1 || width > 18 || current < 0) {
            throw new CodeGeneratorException("Invalid code rule: " + prefix);
        }
        long maximum = maxValue(width);
        if (current >= maximum) {
            throw new CodeGeneratorException("Code sequence exceeds MAX_CODE_WIDTH: " + prefix);
        }
        long next = current + 1;
        int updated = jdbcTemplate.update(RULE_UPDATE_SQL, next, tenantId, prefix, current);
        if (updated != 1) {
            throw new CodeGeneratorException("Code rule changed during allocation: " + prefix);
        }
        return format(prefix, next, width);
    }

    private static long maxValue(int width) {
        long value = 1L;
        for (int i = 0; i < width; i++) {
            value *= 10L;
        }
        return value - 1;
    }

    private static String format(String prefix, long value, int width) {
        String number = Long.toString(value);
        StringBuilder result = new StringBuilder(prefix.length() + width + 1);
        result.append(prefix).append('_');
        for (int i = number.length(); i < width; i++) {
            result.append('0');
        }
        return result.append(number).toString();
    }

    private static void validatePart(String value, String name) {
        if (value == null || !SAFE_PART.matcher(value).matches()) {
            throw new CodeGeneratorException(name + " contains unsupported characters");
        }
    }
}
