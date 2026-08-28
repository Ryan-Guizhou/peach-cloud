package com.peach.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务工具类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/7 14:07
 */
public final class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * 注册一个仅在当前事务成功提交后执行的动作；无活动事务时立即执行。
     *
     * @param action 待执行动作（缓存失效 / 通知 / 注销会话等），为 null 时忽略
     */
    public static void runAfterCommit(Runnable action) {
        if (action == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new AfterCommitSynchronization(action));
        } else {
            action.run();
        }
    }

    /**
     * AfterCommitSynchronization。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    private static final class AfterCommitSynchronization implements TransactionSynchronization {

        private final Runnable action;

        private AfterCommitSynchronization(Runnable action) {
            this.action = action;
        }

        @Override
        public void afterCommit() {
            action.run();
        }
    }
}
