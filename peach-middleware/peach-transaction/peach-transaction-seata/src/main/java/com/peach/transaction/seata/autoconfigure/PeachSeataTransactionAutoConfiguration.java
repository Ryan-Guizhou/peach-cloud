package com.peach.transaction.seata.autoconfigure;

import com.peach.transaction.api.GlobalTransactionExecutor;
import com.peach.transaction.seata.SeataGlobalTransactionExecutor;
import io.seata.tm.api.GlobalTransactionContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Seata Provider 注册。TC、数据源代理和 Feign XID 传播由官方 Starter 接管。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
@AutoConfiguration
@ConditionalOnClass(GlobalTransactionContext.class)
@ConditionalOnProperty(prefix = "peach.transaction", name = "enabled", havingValue = "true")
public class PeachSeataTransactionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalTransactionExecutor.class)
    public GlobalTransactionExecutor seataGlobalTransactionExecutor() {
        return new SeataGlobalTransactionExecutor();
    }
}
