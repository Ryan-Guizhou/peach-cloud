package com.peach.mybatis.autoconfigure;

import com.peach.mybatis.metrics.SqlMetricRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * 有 Micrometer 时记录 SQL 耗时。
 * <p>{@code statement} 标签使用 Mapper 方法全名，便于按语句看耗时。
 * 方法数量很大时，标签基数会随之增加，届时可以收成 Mapper 类名。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "peach.mybatis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PeachMybatisMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(SqlMetricRecorder.class)
    public SqlMetricRecorder sqlMetricRecorder(MeterRegistry meterRegistry) {
        return (statementId, elapsedMillis, slow) -> Timer.builder("peach.mybatis.sql")
                .tag("statement", statementId)
                .tag("slow", Boolean.toString(slow))
                .register(meterRegistry)
                .record(elapsedMillis, TimeUnit.MILLISECONDS);
    }
}
