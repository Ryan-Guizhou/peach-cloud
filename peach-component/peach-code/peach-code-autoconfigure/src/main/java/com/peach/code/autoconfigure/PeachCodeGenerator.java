package com.peach.code.autoconfigure;

import com.peach.code.CodeGenerator;
import com.peach.code.CodeGeneratorException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 以 MySQL 行锁串行化发号，并在锁内同步 Redis 水位。
 *
 * <p>规则每次从数据库读取；账本永久保留历史编码及序号。独立事务提交规则水位和账本后
 * 才向调用者返回，因此业务回滚只产生空号。Redis 丢失、超时或停用时仍由同一数据库
 * 分配路径负责；Redis 绝不会独立向业务发号。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/23 10:45
 */
public class PeachCodeGenerator implements CodeGenerator {

    private static final Pattern SAFE_PART = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,31}");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String RULE_SELECT_SQL = "SELECT CODE_PREFIX, MAX_CODE_WIDTH, CURRENT_VALUE, STATUS "
            + "FROM PEACH_CODE_RULE WHERE TENANT_ID = ? AND CODE_TYPE = ? FOR UPDATE";
    private static final String LEDGER_MAX_SQL = "SELECT MAX(SEQUENCE_VALUE) FROM PEACH_CODE_ISSUED "
            + "WHERE TENANT_ID = ? AND CODE_TYPE = ?";
    private static final String RULE_UPDATE_SQL = "UPDATE PEACH_CODE_RULE SET CURRENT_VALUE = ?, "
            + "MODIFY_TIME = ? "
            + "WHERE TENANT_ID = ? AND CODE_TYPE = ? AND STATUS = 'ENABLE' AND CURRENT_VALUE = ?";
    private static final String LEDGER_INSERT_SQL = "INSERT INTO PEACH_CODE_ISSUED "
            + "(TENANT_ID,CODE_TYPE,CODE_VALUE,SEQUENCE_VALUE,CREATED_TIME) "
            + "VALUES (?,?,?,?,?)";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate allocationTransaction;
    private final CodeSequenceCoordinator coordinator;

    /**
     * 创建仅使用数据库持久化分配的实例。
     *
     * @param jdbcTemplate 连接发号数据库的 JDBC 模板
     * @param properties 发号配置
     * @param transactionManager 必须管理同一数据源的 JDBC 事务管理器
     */
    public PeachCodeGenerator(JdbcTemplate jdbcTemplate, CodeProperties properties,
                              PlatformTransactionManager transactionManager) {
        this(jdbcTemplate, properties, transactionManager, null);
    }

    PeachCodeGenerator(JdbcTemplate jdbcTemplate, CodeProperties properties,
                       PlatformTransactionManager transactionManager, CodeSequenceCoordinator coordinator) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        Objects.requireNonNull(properties, "properties");
        if (!(transactionManager instanceof DataSourceTransactionManager jdbcManager)
                || jdbcManager.getDataSource() != jdbcTemplate.getDataSource()) {
            throw new CodeGeneratorException("Code allocation requires a JDBC transaction manager for the same data source");
        }
        this.coordinator = properties.isRedisEnabled() ? coordinator : null;
        this.allocationTransaction = new TransactionTemplate(transactionManager);
        this.allocationTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.allocationTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    /**
     * 按稳定业务类型读取最新规则并分配编码，阻塞等待数据库行锁。
     *
     * @param tenantId 租户标识
     * @param codeType 稳定类型，例如 MENU、ROLE、RESOURCE、FUNCTION；忽略大小写
     * @return 已独立提交的编码，例如 M00001
     * @throws CodeGeneratorException 参数、规则、水位、编码冲突或数据库事务异常
     */
    @Override
    public String next(String tenantId, String codeType) {
        validatePart(tenantId, "tenantId");
        validatePart(codeType, "codeType");
        String normalizedType = codeType.toUpperCase(Locale.ROOT);
        try {
            return Objects.requireNonNull(allocationTransaction.execute(status -> allocate(tenantId, normalizedType)),
                    "MySQL code allocation returned null");
        } catch (CodeGeneratorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CodeGeneratorException("Failed to allocate business code for type " + normalizedType, ex);
        }
    }

    private String allocate(String tenantId, String codeType) {
        Map<String, Object> row = jdbcTemplate.queryForMap(RULE_SELECT_SQL, tenantId, codeType);
        if (!"ENABLE".equals(row.get("STATUS"))) {
            throw new CodeGeneratorException("Code rule is disabled: " + codeType);
        }
        String prefix = row.get("CODE_PREFIX") instanceof String value ? value : null;
        validatePart(prefix, "CODE_PREFIX");
        if (!(row.get("MAX_CODE_WIDTH") instanceof Number widthNumber)
                || !(row.get("CURRENT_VALUE") instanceof Number currentNumber)) {
            throw new CodeGeneratorException("Invalid code rule values: " + codeType);
        }
        int width = widthNumber.intValue();
        long current = currentNumber.longValue();
        if (width < 1 || width > 18 || current < 0) {
            throw new CodeGeneratorException("Invalid code rule: " + codeType);
        }
        Long issuedMaximum = jdbcTemplate.queryForObject(LEDGER_MAX_SQL, Long.class, tenantId, codeType);
        long durable = Math.max(current, issuedMaximum == null ? 0 : issuedMaximum);
        long maximum = maxValue(width);
        if (durable >= maximum) {
            throw new CodeGeneratorException("Code sequence exceeds MAX_CODE_WIDTH: " + codeType);
        }
        long next = coordinator == null ? durable + 1 : coordinator.next(tenantId, codeType, durable, maximum);
        if (next <= durable || next > maximum) {
            throw new CodeGeneratorException("Code sequence exceeds MAX_CODE_WIDTH: " + codeType);
        }
        String number = Long.toString(next);
        String code = prefix + "0".repeat(width - number.length()) + number;
        String now = LocalDateTime.now().format(TIME_FORMAT);
        // 唯一账本约束同时保护跨类型前缀冲突及历史格式碰撞，冲突时整笔分配失败。
        if (jdbcTemplate.update(LEDGER_INSERT_SQL, tenantId, codeType, code, next, now) != 1
                || jdbcTemplate.update(RULE_UPDATE_SQL, next, now, tenantId, codeType, current) != 1) {
            throw new CodeGeneratorException("Code rule changed during allocation: " + codeType);
        }
        return code;
    }

    private static long maxValue(int width) {
        long value = 1;
        for (int i = 0; i < width; i++) {
            value *= 10;
        }
        return value - 1;
    }

    private static void validatePart(String value, String name) {
        if (value == null || !SAFE_PART.matcher(value).matches()) {
            throw new CodeGeneratorException(name + " contains unsupported characters");
        }
    }
}
