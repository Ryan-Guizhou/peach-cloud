package com.peach.code.autoconfigure;

import com.peach.code.CodeGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Peach代码自动配置。
 * <p>仅在引入 {@code JdbcTemplate}、启用 {@code peach.code} 且业务方没有自定义
 * {@link CodeGenerator} 时创建默认实现。Redis 客户端和事务管理器均采用可选注入，分别用于
 * Redis 主路径和 MySQL 故障兜底。</p>
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
     * 创建默认 JDBC 编码生成器。
     *
     * @param jdbcTemplate 业务数据库 JDBC 模板
     * @param properties 编码生成器配置
     * @param redisProvider 可选的 Redis 字符串模板
     * @param transactionProvider 可选的事务管理器，用于 MySQL 兜底和水位持久化
     * @return 默认编码生成器
     */
    @Bean
    @ConditionalOnMissingBean(CodeGenerator.class)
    public CodeGenerator codeGenerator(JdbcTemplate jdbcTemplate, CodeProperties properties,
                                       ObjectProvider<StringRedisTemplate> redisProvider,
                                       ObjectProvider<org.springframework.transaction.PlatformTransactionManager>
                                               transactionProvider) {
        return new PeachCodeGenerator(jdbcTemplate, properties, redisProvider.getIfAvailable(),
                transactionProvider.getIfAvailable());
    }
}
