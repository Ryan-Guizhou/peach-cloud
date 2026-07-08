package com.peach.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
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
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
