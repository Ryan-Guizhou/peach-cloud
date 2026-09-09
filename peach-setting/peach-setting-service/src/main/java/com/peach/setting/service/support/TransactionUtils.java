package com.peach.setting.service.support;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class TransactionUtils {

    private TransactionUtils() {
    }

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
