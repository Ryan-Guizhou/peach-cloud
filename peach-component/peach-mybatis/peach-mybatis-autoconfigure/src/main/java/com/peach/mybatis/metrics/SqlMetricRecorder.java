package com.peach.mybatis.metrics;

/**
 * SQL 耗时记录。存在 MeterRegistry 时由自动配置提供实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@FunctionalInterface
public interface SqlMetricRecorder {

    /**
     * 记录一次 SQL 耗时。
     *
     * @param statementId Mapper 方法 id
     * @param elapsedMillis 耗时毫秒
     * @param slow 是否超过慢 SQL 阈值
     */
    void record(String statementId, long elapsedMillis, boolean slow);
}
