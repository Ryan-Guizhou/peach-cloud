package com.peach.satoken.autoconfigure;

import com.peach.common.audit.AuditContext;
import com.peach.common.audit.AuditContextProvider;
import com.peach.satoken.support.SatokenAuditContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 审计上下文自动配置，将 Sa-Token 用户上下文桥接到 {@link AuditContext}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
@Slf4j
@AutoConfiguration
public class PeachAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditContextProvider.class)
    public AuditContextProvider satokenAuditContextProvider() {
        SatokenAuditContextProvider provider = new SatokenAuditContextProvider();
        return provider;
    }

    @Bean
    public AuditContextRegistrar auditContextRegistrar(AuditContextProvider provider) {
        return new AuditContextRegistrar(provider);
    }

    public static final class AuditContextRegistrar {

        public AuditContextRegistrar(AuditContextProvider provider) {
            AuditContext.register(provider);
            log.debug("Registered AuditContextProvider");
        }
    }
}
