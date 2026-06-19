package com.peach.fileservice.config;

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


/**
 * SQL 耗时拦截器
 *
 * <p>MyBatis 插件，用于拦截 SQL 执行并记录执行时间。
 * 支持慢查询告警，当 SQL 执行时间超过阈值时输出警告日志。</p>
 *
 * <p>拦截的 SQL 类型：query、update、batch</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = { Statement.class, ResultHandler.class }),
        @Signature(type = StatementHandler.class, method = "update", args = { Statement.class }),
        @Signature(type = StatementHandler.class, method = "batch", args = { Statement.class }) })
public class SqlCostInterceptor implements Interceptor {

    private static Logger log = LoggerFactory.getLogger("SQL_LOG");

    /**
     * 慢查询阈值（毫秒）
     * <p>SQL 执行时间超过此值时输出警告日志，默认3000毫秒（3秒）</p>
     */
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

    /**
     * 拦截 SQL 执行，记录执行时间
     *
     * <p>在 SQL 执行前后记录时间，执行完成后：
     * <ul>
     *   <li>DEBUG 级别：输出所有 SQL 的执行详情（SQL语句、参数、耗时）</li>
     *   <li>WARN 级别：当执行时间超过阈值时，输出慢查询告警</li>
     * </ul>
     *
     * @param invocation 调用信息
     * @return SQL 执行结果
     * @throws Throwable 执行异常
     */
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

    /**
     * 包装目标对象，注册拦截器
     *
     * @param arg0 目标对象
     * @return 包装后的代理对象
     */
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

    /**
     * 生成 SQL 信息（ID、SQL语句、参数）
     *
     * <p>通过反射获取 MyBatis 内部的 MappedStatement 和 BoundSql 对象，
     * 提取 SQL 语句和参数信息。</p>
     *
     * @param statementHandler StatementHandler 对象
     * @return SQL 信息（包含 id、sql、parameter）
     */
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
