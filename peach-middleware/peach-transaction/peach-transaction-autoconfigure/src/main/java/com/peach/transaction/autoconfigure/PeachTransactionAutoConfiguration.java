package com.peach.transaction.autoconfigure;

import com.peach.transaction.api.GlobalTransactionExecutor;
import com.peach.transaction.api.GlobalTransactionOperations;
import com.peach.transaction.api.TransactionMode;
import com.peach.transaction.config.PeachTransactionProperties;
import com.peach.transaction.support.DefaultGlobalTransactionOperations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * 事务 Facade 装配与危险组合安全检查，不创建或替代 Seata 自带协调器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
@AutoConfiguration
@AutoConfigureAfter(name = "com.peach.transaction.seata.autoconfigure.PeachSeataTransactionAutoConfiguration")
@EnableConfigurationProperties(PeachTransactionProperties.class)
@ConditionalOnProperty(prefix = "peach.transaction", name = "enabled", havingValue = "true")
public class PeachTransactionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalTransactionOperations.class)
    public GlobalTransactionOperations peachGlobalTransactionOperations(
            ObjectProvider<GlobalTransactionExecutor> executors,
            PeachTransactionProperties properties,
            Environment environment) {
        if (!StringUtils.hasText(properties.getProvider())) {
            throw new IllegalStateException("peach.transaction.provider must not be blank");
        }
        if (properties.getMode() == null) {
            throw new IllegalStateException("peach.transaction.mode is required");
        }
        if (properties.getTimeoutMillis() <= 0 || properties.getTimeoutMillis() > 600_000) {
            throw new IllegalStateException("peach.transaction.timeout-millis must be within 1-600000");
        }
        if (properties.getMode() == TransactionMode.AT
                && environment.getProperty("peach.rocket.outbox.enabled", Boolean.class, false)
                && !properties.isAllowAtOutboxCoexistence()) {
            throw new IllegalStateException("Seata AT and Rocket Outbox enabled in one service: "
                    + "unsafe unless global-commit-aware dispatch is established; "
                    + "see peach.transaction.allow-at-outbox-coexistence");
        }
        return new DefaultGlobalTransactionOperations(executors.orderedStream().toList(), properties);
    }
}
