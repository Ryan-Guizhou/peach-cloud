package com.peach.mybatis.interceptor;

import com.peach.mybatis.config.PeachMybatisProperties;
import com.peach.mybatis.metrics.SqlMetricRecorder;
import com.peach.mybatis.sql.ExecutableSqlRenderer;
import com.peach.mybatis.sql.SqlParameters;
import com.peach.mybatis.sql.SqlTexts;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 记录 SQL 耗时，并输出普通 SQL 与慢 SQL。
 * <p>默认 Preparing 保留 {@code ?}，Parameters 按占位符顺序列出参数值。
 * {@code peach.mybatis.log.executable=true} 时 Preparing 改为可执行 SQL，且不再输出 Parameters。
 * 同一次调用只输出其中一种。慢 SQL 使用 WARN，其余语句在 DEBUG 输出。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
        @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})
})
public class SqlCostInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger("SQL_LOG");

    private final PeachMybatisProperties properties;

    private final SqlMetricRecorder metricRecorder;

    private final String lineSeparator = System.lineSeparator();

    public SqlCostInterceptor(PeachMybatisProperties properties, SqlMetricRecorder metricRecorder) {
        this.properties = properties;
        this.metricRecorder = metricRecorder;
        log.info("SQL logging enabled: longQueryTime={}ms, executable={}",
                properties.getLongQueryTime(), properties.getLog().isExecutable());
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Optional<SqlDetail> detail = detail(invocation.getTarget());
        if (detail.isEmpty()) {
            return invocation.proceed();
        }
        SqlDetail sqlDetail = detail.get();
        warnMissingWhere(sqlDetail);
        StopWatch watch = new StopWatch(sqlDetail.id);
        watch.start();
        Object result = null;
        try {
            result = invocation.proceed();
            return result;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            long elapsed = watch.getTotalTimeMillis();
            boolean slow = elapsed > properties.getLongQueryTime();
            writeLog(sqlDetail, watch, slow);
            warnLargeResult(sqlDetail.id, result);
            if (metricRecorder != null) {
                metricRecorder.record(sqlDetail.id, elapsed, slow);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    private void writeLog(SqlDetail sqlDetail, StopWatch watch, boolean slow) {
        try {
            if (!slow && !log.isDebugEnabled()) {
                return;
            }
            StatementText text = statementText(sqlDetail);
            if (slow) {
                log.warn("!!!slow query->{}!!!", sqlDetail.id);
                writeStatement(true, sqlDetail.id, text, watch);
                return;
            }
            writeStatement(false, sqlDetail.id, text, watch);
        } catch (Exception exception) {
            log.warn("Failed to write SQL log: {}", sqlDetail.id, exception);
        }
    }

    /**
     * 按开关选择一种语句文本。只有真正输出日志时才读取参数。
     * executable 为 true 时 parameters 为 null，调用方不能再拼 Parameters 行。
     */
    private StatementText statementText(SqlDetail sqlDetail) {
        if (properties.getLog().isExecutable()) {
            return new StatementText(ExecutableSqlRenderer.render(sqlDetail.sql, sqlDetail.values()), null);
        }
        return new StatementText(sqlDetail.sql, SqlParameters.format(sqlDetail.values()));
    }

    private void writeStatement(boolean warn, String statementId, StatementText text, StopWatch watch) {
        String statement = warn ? text.statement() : limit(text.statement());
        if (warn) {
            log.warn("==>{}", statementId);
            log.warn("==>  Preparing: {}{}", lineSeparator, statement);
            if (text.parameters() != null) {
                log.warn("==> Parameters: {}", text.parameters());
            }
            log.warn("{}{}", lineSeparator, watch.prettyPrint());
            return;
        }
        log.debug("==>{}", statementId);
        log.debug("==>  Preparing: {}{}", lineSeparator, statement);
        if (text.parameters() != null) {
            log.debug("==> Parameters: {}", text.parameters());
        }
        log.debug("{}{}", lineSeparator, watch.prettyPrint());
    }

    /**
     * 只截断 DEBUG 的 Preparing 文本。慢 SQL 和缺少 WHERE 的告警保持完整。
     */
    private String limit(String statement) {
        int maxLength = properties.getLog().getMaxSqlLength();
        if (statement == null || maxLength <= 0 || statement.length() <= maxLength) {
            return statement;
        }
        return statement.substring(0, maxLength) + "...";
    }

    private void warnMissingWhere(SqlDetail sqlDetail) {
        if (!properties.getLog().isWarnMissingWhere() || !SqlTexts.missingWhere(sqlDetail.sql)) {
            return;
        }
        String statement = properties.getLog().isExecutable() ? statementText(sqlDetail).statement() : sqlDetail.sql;
        log.warn("!!!missing where->{}!!!{}{}", sqlDetail.id, lineSeparator, statement);
    }

    private void warnLargeResult(String statementId, Object result) {
        int threshold = properties.getLog().getLargeResultThreshold();
        if (threshold > 0 && result instanceof Collection<?> rows && rows.size() > threshold) {
            log.warn("!!!large result->{} size={}!!!", statementId, rows.size());
        }
    }

    private Optional<SqlDetail> detail(Object statementHandler) {
        if (!(statementHandler instanceof StatementHandler handler)) {
            return Optional.empty();
        }
        try {
            MetaObject routingMeta = SystemMetaObject.forObject(handler);
            Object delegate = routingMeta.hasGetter("delegate") ? routingMeta.getValue("delegate") : handler;
            if (!(delegate instanceof StatementHandler delegateHandler)) {
                return Optional.empty();
            }
            MetaObject delegateMeta = SystemMetaObject.forObject(delegateHandler);
            MappedStatement mappedStatement = (MappedStatement) delegateMeta.getValue("mappedStatement");
            BoundSql boundSql = delegateHandler.getBoundSql();
            return Optional.of(new SqlDetail(mappedStatement.getId(), boundSql.getSql(), mappedStatement, boundSql));
        } catch (Exception exception) {
            log.warn("Failed to read SQL for logging", exception);
            return Optional.empty();
        }
    }

    /**
     * 执行前复制的语句。sql 在分页改写前取得，参数等到确实要输出日志时再从这份 BoundSql 读取。
     */
    private static final class SqlDetail {

        private final String id;

        private final String sql;

        private final MappedStatement mappedStatement;

        private final BoundSql boundSql;

        private List<Object> values;

        private SqlDetail(String id, String sql, MappedStatement mappedStatement, BoundSql boundSql) {
            this.id = id;
            this.sql = sql;
            this.mappedStatement = mappedStatement;
            this.boundSql = boundSql;
        }

        private List<Object> values() {
            if (values == null) {
                values = SqlParameters.values(mappedStatement, boundSql);
            }
            return values;
        }
    }

    /**
     * 一次日志使用的语句。parameters 为 null 表示当前是可执行语句，不再输出 Parameters。
     *
     * @param statement 带 {@code ?} 的 SQL，或已经替换参数的可执行 SQL
     * @param parameters 与 {@code ?} 顺序一致的参数；可执行模式下为 null
     */
    private record StatementText(String statement, String parameters) {
    }
}
