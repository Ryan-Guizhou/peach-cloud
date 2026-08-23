package com.peach.code.autoconfigure;

import com.peach.code.CodeGenerator;
import com.peach.code.CodeGeneratorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Redis 优先、MySQL 兜底的租户业务编码生成器。
 *
 * <p>正常路径使用 Lua 在 Redis 中原子完成“以 MySQL 当前值校准 + 自增 + 最大值校验”；
 * Redis 不可用时使用 MySQL {@code LAST_INSERT_ID(expr)} 原子自增，并在成功后尝试回写 Redis。
 * Redis 分配成功后还会通过独立事务将 MySQL 规则表只增持久化，缩小 Redis 丢失后的恢复窗口。
 * 应用启动时会将规则表中的 MySQL 序号与 Redis 序号取最大值同步，避免 Redis 恢复后回退。</p>
 *
 * <p>该模式与业务事务不构成分布式事务：Redis 已分配的序号不会因业务事务回滚而回收。
 * 如果业务必须严格避免回滚空号，应关闭 {@code peach.code.redis-enabled}，使用 MySQL 事务模式。</p>
 */
@Slf4j
public class PeachCodeGenerator implements CodeGenerator {

    private static final Pattern SAFE_PART = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,31}");

    /** 初始化、校准并递增序号，保证 key 缺失和并发初始化只有一个结果。 */
    private static final RedisScript<Long> INCREMENT_SCRIPT = RedisScript.of("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local base = tonumber(ARGV[1])
            if current < base then current = base end
            local next = current + 1
            if next > tonumber(ARGV[2]) then return -1 end
            redis.call('SET', KEYS[1], tostring(next))
            return next
            """, Long.class);

    /** 仅在目标水位更大时更新 Redis，防止故障恢复期间水位回退。 */
    private static final RedisScript<Long> SET_IF_GREATER_SCRIPT = RedisScript.of("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local target = tonumber(ARGV[1])
            if current < target then
              redis.call('SET', KEYS[1], ARGV[1])
              return target
            end
            return current
            """, Long.class);

    private final JdbcTemplate jdbcTemplate;
    private final CodeProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate mysqlFallbackTransaction;

    /**
     * 创建 Redis 优先的业务编码生成器。
     *
     * @param jdbcTemplate 业务数据库 JDBC 模板
     * @param properties 编码配置
     * @param redisTemplate Redis 字符串模板，可为空
     * @param transactionManager MySQL 兜底事务管理器，可为空
     */
    public PeachCodeGenerator(JdbcTemplate jdbcTemplate, CodeProperties properties,
                              StringRedisTemplate redisTemplate,
                              PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.mysqlFallbackTransaction = transactionManager == null ? null
                : new TransactionTemplate(transactionManager);
        if (this.mysqlFallbackTransaction != null) {
            this.mysqlFallbackTransaction.setPropagationBehavior(
                    TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        }
    }

    /**
     * 启动时同步已配置规则的 Redis 和 MySQL 序号。
     *
     * <p>同步采用两端最大值：Redis 领先时抬高 MySQL，MySQL 领先时单调抬高 Redis。
     * Redis 不可用不会阻止应用启动，后续请求会自动进入 MySQL 兜底路径。</p>
     */
    @PostConstruct
    public void synchronizeCounters() {
        if (!properties.isRedisEnabled() || redisTemplate == null) {
            return;
        }
        List<Map<String, Object>> rules = jdbcTemplate.queryForList(
                "SELECT TENANT_ID, CODE_PREFIX, CURRENT_VALUE FROM PEACH_CODE_RULE "
                        + "WHERE STATUS = 'ENABLE'");
        if (CollectionUtils.isEmpty(rules)) {
            log.info("rules is null");
            return;
        }
        for (Map<String, Object> rule : rules) {
            String tenantId = String.valueOf(rule.get("TENANT_ID"));
            String prefix = String.valueOf(rule.get("CODE_PREFIX"));
            long mysqlValue = ((Number) rule.get("CURRENT_VALUE")).longValue();
            try {
                String key = redisKey(tenantId, prefix);
                Long redisValue = readRedisValue(key);
                long maxValue = Math.max(mysqlValue, redisValue == null ? 0L : redisValue);
                if (redisValue == null && mysqlValue > 0L) {
                    log.warn("Redis code counter is missing. stage=startup-sync, tenantId={}, prefix={}, "
                                    + "mysqlValue={}, action=restore-redis",
                            tenantId, prefix, mysqlValue);
                } else if (redisValue != null && redisValue < mysqlValue) {
                    log.warn("Redis code counter is behind MySQL. stage=startup-sync, tenantId={}, prefix={}, "
                                    + "redisValue={}, mysqlValue={}, action=advance-redis",
                            tenantId, prefix, redisValue, mysqlValue);
                } else if (redisValue != null && redisValue > mysqlValue) {
                    log.warn("Redis code counter is ahead of MySQL. stage=startup-sync, tenantId={}, prefix={}, "
                                    + "redisValue={}, mysqlValue={}, action=advance-mysql",
                            tenantId, prefix, redisValue, mysqlValue);
                }
                if (maxValue > mysqlValue) {
                    jdbcTemplate.update(
                            "UPDATE PEACH_CODE_RULE SET CURRENT_VALUE = ?, "
                                    + "MODIFY_TIME = DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s') "
                                    + "WHERE TENANT_ID = ? AND CODE_PREFIX = ? AND CURRENT_VALUE < ?",
                            maxValue, tenantId, prefix, maxValue);
                }
                if (maxValue > 0L) {
                    setRedisIfGreater(key, maxValue);
                }
            } catch (Exception ex) {
                log.warn("Code counter synchronization failed. stage=startup-sync, tenantId={}, prefix={}, "
                                + "action=continue-with-current-state",
                        tenantId, prefix, ex);
            }
        }
    }

    /**
     * 生成下一个租户业务编码。
     *
     * @param tenantId 租户标识
     * @param prefix 编码前缀
     * @return 格式为“前缀_数字部分”的编码
     * @throws CodeGeneratorException 规则不存在、参数非法、序号溢出或 Redis/MySQL 均不可用时抛出
     */
    @Override
    public String next(String tenantId, String prefix) {
        validatePart(tenantId, "tenantId");
        validatePart(prefix, "prefix");
        CodeRule rule = readRule(tenantId, prefix);
        long maxValue = maxValue(rule.maxCodeWidth);

        if (properties.isRedisEnabled() && redisTemplate != null) {
            try {
                String key = redisKey(tenantId, prefix);
                Long redisValue = readRedisValue(key);
                if (redisValue == null && rule.currentValue > 0L) {
                    log.warn("Redis code counter is missing. stage=allocate, tenantId={}, prefix={}, "
                                    + "mysqlValue={}, action=restore-and-increment",
                            tenantId, prefix, rule.currentValue);
                } else if (redisValue != null && redisValue < rule.currentValue) {
                    log.warn("Redis code counter is behind MySQL. stage=allocate, tenantId={}, prefix={}, "
                                    + "redisValue={}, mysqlValue={}, action=advance-and-increment",
                            tenantId, prefix, redisValue, rule.currentValue);
                }
                Long value = redisTemplate.execute(INCREMENT_SCRIPT,
                        List.of(key),
                        Long.toString(rule.currentValue), Long.toString(maxValue));
                if (value == null || value < 0L) {
                    throw new CodeGeneratorException("Code sequence exceeds MAX_CODE_WIDTH: " + prefix);
                }
                try {
                    persistMysqlWatermark(tenantId, prefix, value);
                } catch (Exception persistEx) {
                    log.error("MySQL watermark persistence failed after Redis allocation. "
                                    + "stage=allocate, tenantId={}, prefix={}, redisValue={}, action=fail-closed",
                            tenantId, prefix, value, persistEx);
                    throw new CodeGeneratorException("Failed to persist Redis code watermark: " + prefix,
                            persistEx);
                }
                log.debug("Business code allocated. source=redis, tenantId={}, prefix={}, sequence={}, code={}",
                        tenantId, prefix, value, format(prefix, value, rule.maxCodeWidth));
                return format(prefix, value, rule.maxCodeWidth);
            } catch (CodeGeneratorException ex) {
                throw ex;
            } catch (Exception redisEx) {
                log.warn("Redis code allocation failed. stage=allocate, tenantId={}, prefix={}, "
                                + "action=mysql-fallback",
                        tenantId, prefix, redisEx);
            }
        }

        long value = incrementFromMysql(tenantId, prefix);
        if (value > maxValue) {
            throw new CodeGeneratorException("Code sequence exceeds MAX_CODE_WIDTH: " + prefix);
        }
        if (properties.isRedisEnabled() && redisTemplate != null) {
            try {
                setRedisIfGreater(redisKey(tenantId, prefix), value);
            } catch (Exception ex) {
                log.warn("Redis code counter write-back failed. stage=mysql-fallback, tenantId={}, prefix={}, "
                                + "mysqlValue={}, action=continue",
                        tenantId, prefix, value, ex);
            }
        }
        String code = format(prefix, value, rule.maxCodeWidth);
        log.info("Business code allocated. source=mysql-fallback, tenantId={}, prefix={}, sequence={}, code={}",
                tenantId, prefix, value, code);
        return code;
    }

    /**
     * 将 Redis 已分配序号以独立事务只增写入 MySQL，避免外层业务回滚降低事实源水位。
     *
     * @param tenantId 租户标识
     * @param prefix 编码前缀
     * @param value Redis 已分配序号
     */
    private void persistMysqlWatermark(String tenantId, String prefix, long value) {
        if (mysqlFallbackTransaction == null) {
            throw new CodeGeneratorException("No transaction manager available for MySQL watermark");
        }
        mysqlFallbackTransaction.execute(status -> {
            jdbcTemplate.update(
                    "UPDATE PEACH_CODE_RULE SET CURRENT_VALUE = ?, "
                            + "MODIFY_TIME = DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s') "
                            + "WHERE TENANT_ID = ? AND CODE_PREFIX = ? AND CURRENT_VALUE < ?",
                    value, tenantId, prefix, value);
            return null;
        });
    }

    /**
     * 使用独立 MySQL 事务原子递增序号。
     *
     * @param tenantId 租户标识
     * @param prefix 编码前缀
     * @return MySQL 分配的序号
     */
    private long incrementFromMysql(String tenantId, String prefix) {
        if (mysqlFallbackTransaction == null) {
            throw new CodeGeneratorException("No transaction manager available for MySQL fallback");
        }
        Long value = mysqlFallbackTransaction.execute(status -> {
            int updated = jdbcTemplate.update(
                    "UPDATE PEACH_CODE_RULE SET CURRENT_VALUE = LAST_INSERT_ID(CURRENT_VALUE + 1), "
                            + "MODIFY_TIME = DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s') "
                            + "WHERE TENANT_ID = ? AND CODE_PREFIX = ? AND STATUS = 'ENABLE'",
                    tenantId, prefix);
            if (updated != 1) {
                throw new CodeGeneratorException("Code rule is missing or disabled: " + prefix);
            }
            Long current = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            if (current == null) {
                throw new CodeGeneratorException("MySQL sequence returned null: " + prefix);
            }
            return current;
        });
        if (value == null) {
            throw new CodeGeneratorException("MySQL sequence transaction returned null: " + prefix);
        }
        return value;
    }

    /**
     * 从 MySQL 事实源读取并校验指定租户、前缀的编码规则。
     *
     * @param tenantId 租户标识
     * @param prefix 编码前缀
     * @return 已启用且参数合法的编码规则
     * @throws CodeGeneratorException 规则不存在、已停用、配置非法或查询失败时抛出
     */
    private CodeRule readRule(String tenantId, String prefix) {
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT MAX_CODE_WIDTH, CURRENT_VALUE, STATUS FROM PEACH_CODE_RULE "
                            + "WHERE TENANT_ID = ? AND CODE_PREFIX = ?",
                    tenantId, prefix);
            String status = String.valueOf(row.get("STATUS"));
            if (!"ENABLE".equalsIgnoreCase(status)) {
                throw new CodeGeneratorException("Code rule is disabled: " + prefix);
            }
            int width = ((Number) row.get("MAX_CODE_WIDTH")).intValue();
            long current = ((Number) row.get("CURRENT_VALUE")).longValue();
            if (width <= 0 || width > 18 || current < 0L) {
                throw new CodeGeneratorException("Invalid code rule: " + prefix);
            }
            return new CodeRule(width, current);
        } catch (CodeGeneratorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CodeGeneratorException("Failed to load code rule: " + prefix, ex);
        }
    }

    /**
     * 读取 Redis 当前序号。
     *
     * <p>键不存在时返回 {@code null}，以便调用方区分“尚未初始化或被删除”和数值 {@code 0}；
     * Redis 值不是合法长整型时由调用方按 Redis 故障路径处理。</p>
     *
     * @param key Redis 序列键
     * @return 当前序号；键不存在时返回 {@code null}
     */
    private Long readRedisValue(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? null : Long.valueOf(value);
    }

    /**
     * 将 Redis 序号单调推进至指定水位，避免并发恢复时覆盖更大的序号。
     *
     * @param key Redis 序列键
     * @param value 待写入的最小水位
     */
    private void setRedisIfGreater(String key, long value) {
        redisTemplate.execute(SET_IF_GREATER_SCRIPT, List.of(key), Long.toString(value));
    }

    /**
     * 根据租户和编码前缀构造 Redis 序列键。
     *
     * @param tenantId 租户标识
     * @param prefix 编码前缀
     * @return 规范化后的 Redis 序列键
     */
    private String redisKey(String tenantId, String prefix) {
        return properties.getRedisKeyPrefix() + tenantId + ":" + prefix.toUpperCase(Locale.ENGLISH);
    }

    /**
     * 计算数字部分在指定最大补零宽度内可表示的最大值。
     *
     * @param width 数字部分最大补零宽度
     * @return 可表示的最大序号
     */
    private long maxValue(int width) {
        long value = 1L;
        for (int i = 0; i < width; i++) {
            value *= 10L;
        }
        return value - 1L;
    }

    /**
     * 将序号格式化为“前缀_补零数字”的业务编码。
     *
     * @param prefix 编码前缀
     * @param value 已分配序号
     * @param width 数字部分最大补零宽度
     * @return 格式化后的业务编码
     */
    private String format(String prefix, long value, int width) {
        String number = Long.toString(value);
        StringBuilder result = new StringBuilder(width);
        for (int i = number.length(); i < width; i++) {
            result.append('0');
        }
        result.append(number);
        return prefix.toUpperCase(Locale.ENGLISH) + "_" + result;
    }

    /**
     * 校验租户标识或编码前缀，确保其可以安全参与 Redis 键构造。
     *
     * @param value 待校验内容
     * @param name 参数名称，用于异常定位
     * @throws CodeGeneratorException 参数为空或包含不支持字符时抛出
     */
    private void validatePart(String value, String name) {
        if (value == null || !SAFE_PART.matcher(value).matches()) {
            throw new CodeGeneratorException(name + " contains unsupported characters");
        }
    }

    /**
     * 从 MySQL 规则表读取的最小规则快照。
     *
     * <p>快照只在单次发号过程中使用，避免将数据库行对象泄漏到生成器内部流程。</p>
     */
    private static final class CodeRule {
        private final int maxCodeWidth;
        private final long currentValue;

        private CodeRule(int maxCodeWidth, long currentValue) {
            this.maxCodeWidth = maxCodeWidth;
            this.currentValue = currentValue;
        }
    }
}
