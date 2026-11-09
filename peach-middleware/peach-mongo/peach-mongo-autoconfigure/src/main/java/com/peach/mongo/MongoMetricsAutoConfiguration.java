package com.peach.mongo;

import com.peach.mongo.log.MongoCommandMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * 有 Micrometer 时记录 Mongo 命令耗时。
 * <p>标签使用命令名和集合名。集合数量很大时，时间序列会随之增加。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
public class MongoMetricsAutoConfiguration {

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(MongoCommandMetrics.class)
    public MongoCommandMetrics mongoCommandMetrics(MeterRegistry meterRegistry) {
        return (command, collection, elapsedMillis, slow) -> Timer.builder("peach.mongo.command")
                .tag("command", command == null ? "" : command)
                .tag("collection", collection == null ? "" : collection)
                .tag("slow", Boolean.toString(slow))
                .register(meterRegistry)
                .record(elapsedMillis, TimeUnit.MILLISECONDS);
    }
}
