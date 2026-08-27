package com.peach.fileservice.config;

import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
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

/**
 * MyBatis SQL 耗时拦截器。
 *
 * <p>通过 MyBatis {@link MetaObject} 读取 {@code RoutingStatementHandler} 的 delegate 与
 * mappedStatement，避免直接 {@code Field.setAccessible}。</p>
 */
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = { Statement.class, ResultHandler.class }),
        @Signature(type = StatementHandler.class, method = "update", args = { Statement.class }),
        @Signature(type = StatementHandler.class, method = "batch", args = { Statement.class }) })
public class SqlCostInterceptor implements Interceptor {

    private static final String PARAMETER_KEY = "parameter";

    private static Logger log = LoggerFactory.getLogger("SQL_LOG");

    // 慢查时间, 默认3秒， 单位毫秒
    private int longQueryTime;

    private String lineSeparator = System.getProperty("line.separator");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Optional<Map<String, Object>> sqlInfo = genSqlInfo(invocation.getTarget());
        if (sqlInfo.isEmpty()) {
            return invocation.proceed();
        }
        Map<String, Object> info = sqlInfo.get();
        StopWatch watch = new StopWatch(String.valueOf(info.get("id")));
        watch.start();
        try {
            return invocation.proceed();
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (log.isDebugEnabled()) {
                log.debug("==>{}", info.get("id"));
                log.debug("==>  Preparing: {}{}", lineSeparator, info.get("sql"));
                log.debug("==> Parameters: {}", info.get(PARAMETER_KEY));
                if (log.isDebugEnabled()) {
                    log.debug("\n{}", watch.prettyPrint());
                }
            }
            if (watch.getTotalTimeMillis() > longQueryTime) {
                log.warn("!!!slow query->{}!!!", info.get("id"));
                log.warn("==>{}", info.get("id"));
                log.warn("==>  Preparing: {}{}", lineSeparator, info.get("sql"));
                log.warn("==> Parameters: {}", info.get(PARAMETER_KEY));
                if (log.isWarnEnabled()) {
                    log.warn("\n{}", watch.prettyPrint());
                }
            }
        }
    }

    @Override
    public Object plugin(Object arg0) {
        return Plugin.wrap(arg0, this);
    }

    /**
     * 设置拦截器属性
     *
     * <p>从 MyBatis 配置中读取慢查询阈值参数。</p>
     *
     * @param arg0 属性配置
     */
    @Override
    public void setProperties(Properties arg0) {
        longQueryTime = Integer.parseInt(arg0.getProperty("longQueryTime", "3000"));
    }

    private Optional<Map<String, Object>> genSqlInfo(Object statementHandler) {
        if (!(statementHandler instanceof StatementHandler routingHandler)) {
            return Optional.empty();
        }
        try {
            MetaObject routingMeta = SystemMetaObject.forObject(routingHandler);
            Object delegate = routingMeta.getValue("delegate");
            if (!(delegate instanceof StatementHandler delegateHandler)) {
                return Optional.empty();
            }
            MetaObject delegateMeta = SystemMetaObject.forObject(delegateHandler);
            MappedStatement mappedStatement = (MappedStatement) delegateMeta.getValue("mappedStatement");
            Map<String, Object> result = new HashMap<>();
            result.put("id", mappedStatement.getId());
            result.put("sql", delegateHandler.getBoundSql().getSql());
            result.put(PARAMETER_KEY, delegateHandler.getBoundSql().getParameterObject());
            return Optional.of(result);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

}
