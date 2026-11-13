package com.peach.rocket.jdbc;

import com.peach.rocket.exception.MqException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 核对 Spring 本地 JDBC 事务对指定数据源连接的实际所有权。
 *
 * <p>当前 Spring 6 基线通过受保护的 ConnectionHolder.isTransactionActive 标记区分事务管理器持有的
 * 连接与仅参与生命周期同步的连接。该访问器仅供本地 JDBC 事务使用，不支持通用 JTA 事务；
 * Spring 升级或模块访问限制使标记不可读取时必须拒绝写入，不能降级为仅检查资源绑定或 autoCommit。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
final class JdbcLocalTransactionVerifier {

    private static final Method TRANSACTION_ACTIVE = transactionActiveAccessor();

    private JdbcLocalTransactionVerifier() {
    }

    /**
     * 仅当实际事务、管理器所有权标记和连接事务状态同时有效时返回 true。
     * 无法读取内部标记或连接状态时抛出不含连接信息的异常，禁止静默放行。
     */
    static boolean participates(DataSource dataSource) {
        if (!TransactionSynchronizationManager.isActualTransactionActive() || dataSource == null) {
            return false;
        }
        if (TRANSACTION_ACTIVE == null) {
            throw new MqException("Cannot verify Spring local JDBC transaction ownership");
        }
        Object resource = TransactionSynchronizationManager.getResource(dataSource);
        if (!(resource instanceof ConnectionHolder holder) || holder.getConnectionHandle() == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(TRANSACTION_ACTIVE.invoke(holder))
                    && !holder.getConnection().getAutoCommit();
        } catch (ReflectiveOperationException | SQLException | RuntimeException unavailable) {
            throw new MqException("Cannot verify Spring local JDBC transaction ownership");
        }
    }

    private static Method transactionActiveAccessor() {
        try {
            Method method = ConnectionHolder.class.getDeclaredMethod("isTransactionActive");
            return method.trySetAccessible() ? method : null;
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            return null;
        }
    }
}
