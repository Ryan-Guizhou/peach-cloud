package com.peach.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import com.peach.database.migration.V1__Peach_schema;
import com.peach.database.migration.V5__Peach_system_seed;

/**
 * 项目共享数据库迁移配置，在业务访问数据库前由 Boot 执行；不提供清库和自动修复入口。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 22:00
 */
@AutoConfiguration(before = FlywayAutoConfiguration.class)
@ConditionalOnClass(Flyway.class)
public class PeachDatabaseAutoConfiguration {
    /** 固定项目迁移位置与基线策略，历史迁移不可通过外部配置跳过。 */
    public static FluentConfiguration configure(FluentConfiguration configuration) {
        return configuration.locations("classpath:db/peach/migration")
                .table("PEACH_FLYWAY_HISTORY").baselineOnMigrate(true).baselineVersion("0")
                .validateOnMigrate(true).validateMigrationNaming(true).cleanDisabled(true)
                .outOfOrder(false).placeholderReplacement(false).lockRetryCount(120)
                .javaMigrations(new V1__Peach_schema(), new V5__Peach_system_seed());
    }
    @Bean
    FlywayConfigurationCustomizer peachFlywayConfiguration() { return PeachDatabaseAutoConfiguration::configure; }
    @Bean
    @ConditionalOnMissingBean(FlywayMigrationStrategy.class)
    FlywayMigrationStrategy peachFlywayMigrationStrategy() { return new PeachMigrationStrategy(); }
}
