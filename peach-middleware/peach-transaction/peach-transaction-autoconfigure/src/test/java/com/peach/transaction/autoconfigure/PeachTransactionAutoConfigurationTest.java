package com.peach.transaction.autoconfigure;

import com.peach.transaction.api.GlobalTransactionExecutor;
import com.peach.transaction.api.GlobalTransactionOperations;
import com.peach.transaction.api.TransactionMode;
import com.peach.transaction.api.TransactionOptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starter 条件装配、SPI 覆盖和危险组合的定向回归。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
class PeachTransactionAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PeachTransactionAutoConfiguration.class));

    @Test
    void disabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(GlobalTransactionOperations.class));
    }

    @Test
    void rejectsMissingProviderWhenEnabled() {
        runner.withPropertyValues("peach.transaction.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsAtOutboxCombinationWithoutCommitFence() {
        runner.withPropertyValues("peach.transaction.enabled=true",
                        "peach.rocket.outbox.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void supportsCustomProvider() {
        runner.withPropertyValues("peach.transaction.enabled=true", "peach.transaction.provider=custom")
                .withBean(GlobalTransactionExecutor.class, () -> new GlobalTransactionExecutor() {
                    @Override public String provider() { return "custom"; }
                    @Override public boolean supports(TransactionMode mode) { return mode == TransactionMode.AT; }
                    @Override public <T> T execute(TransactionOptions options,
                                                      java.util.concurrent.Callable<T> action) throws Exception {
                        return action.call();
                    }
                }).run(context -> {
                    GlobalTransactionOperations operations = context.getBean(GlobalTransactionOperations.class);
                    assertEquals("success", operations.execute(
                            new TransactionOptions("test", TransactionMode.AT, 1000), () -> "success"));
                });
    }
}
