package com.peach.message.config;

import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;


@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = { Statement.class, ResultHandler.class }),
        @Signature(type = StatementHandler.class, method = "update", args = { Statement.class }),
        @Signature(type = StatementHandler.class, method = "batch", args = { Statement.class }) })
public class SqlCostInterceptor implements Interceptor {
    private static Logger log = LoggerFactory.getLogger("SQL_LOG");

    // 慢查时间, 默认3秒， 单位毫秒
    private int longQueryTime;

    private static Field DELEGATE_FIELD;

    private static Field MAPPEDSTATEMENTD_FIELD;
    
    private String lineSeparator = System.getProperty("line.separator");

    static {
        try {
            DELEGATE_FIELD = RoutingStatementHandler.class.getDeclaredField("delegate");
            DELEGATE_FIELD.setAccessible(true);
            MAPPEDSTATEMENTD_FIELD = BaseStatementHandler.class.getDeclaredField("mappedStatement");
            MAPPEDSTATEMENTD_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StopWatch watch = null;
        Map info = new HashMap();
        try {
            info = genSqlInfo(invocation.getTarget()).get();
            watch = new StopWatch((String) info.get("id"));
            watch.start();
            Object value = invocation.proceed();
            watch.stop();
            return value;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (log.isDebugEnabled()) {
                log.debug("==>" + (String)info.get("id"));
                log.debug("==>  Preparing: " + lineSeparator + info.get("sql"));
                log.debug("==> Parameters: " + info.get("parameter"));
                log.debug(watch.prettyPrint());
            }
            if (watch.getTotalTimeMillis() > longQueryTime) {
                log.warn("!!!slow query->" + info.get("id") + "!!!");
                log.warn("==>" + info.get("id"));
                log.warn("==>  Preparing: " + lineSeparator + info.get("sql"));
                log.warn("==> Parameters: " + info.get("parameter"));
                log.warn(watch.prettyPrint());
            }
        }
    }

    @Override
    public Object plugin(Object arg0) {
        return Plugin.wrap(arg0, this);
    }

    @Override
    public void setProperties(Properties arg0) {
        longQueryTime = Integer.parseInt(arg0.getProperty("longQueryTime", "3000"));
    }

    private Optional<Map<String, Object>> genSqlInfo(Object statementHandler) {
        Map<String, Object> result = new HashMap();
        try {
            if (statementHandler instanceof StatementHandler) {
                StatementHandler delegateHandler = (StatementHandler) DELEGATE_FIELD.get(statementHandler);
                MappedStatement mappedStatement = (MappedStatement) MAPPEDSTATEMENTD_FIELD.get(delegateHandler);
                result.put("id", mappedStatement.getId());
                result.put("sql", delegateHandler.getBoundSql().getSql());
                result.put("parameter", delegateHandler.getBoundSql().getParameterObject());
            }
            return Optional.of(result);
        } catch (Exception ex) {
            return Optional.of(result);
        }
    }

}