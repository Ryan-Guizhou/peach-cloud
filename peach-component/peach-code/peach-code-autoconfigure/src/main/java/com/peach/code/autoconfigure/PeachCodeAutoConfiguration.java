package com.peach.code.autoconfigure;

import com.peach.code.CodeGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Peach代码自动配置。
 * <p>仅在引入 {@code JdbcTemplate}、启用 {@code peach.code} 且业务方没有自定义
 * {@link CodeGenerator} 时创建默认实现。发号必须使用 MySQL 事务管理器。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@AutoConfiguration
@ConditionalOnClass({JdbcTemplate.class, CodeGenerator.class})
@ConditionalOnProperty(prefix = "peach.code", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(CodeProperties.class)
public class PeachCodeAutoConfiguration {

    /**
     * 创建默认 MySQL 唯一事实源编码生成器。
     *
     * @param jdbcTemplate 业务数据库 JDBC 模板
     * @param properties 编码生成器配置
     * @param transactionProvider MySQL 发号所需的事务管理器
     * @return 默认编码生成器
     */
    @Bean
    @ConditionalOnBean(JdbcTemplate.class)
    @ConditionalOnMissingBean(CodeGenerator.class)
    public CodeGenerator codeGenerator(JdbcTemplate jdbcTemplate, CodeProperties properties,
                                       ObjectProvider<org.springframework.transaction.PlatformTransactionManager>
                                               transactionProvider) {
        return new PeachCodeGenerator(jdbcTemplate, properties, transactionProvider.getIfAvailable());
    }
}
