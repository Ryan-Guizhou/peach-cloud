package com.peach.mongo.log;

/**
 * Mongo 命令耗时记录。存在 MeterRegistry 时由自动配置提供实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/9 16:40
 */
@FunctionalInterface
public interface MongoCommandMetrics {

    /**
     * 记录一次命令耗时。
     *
     * @param command 命令名
     * @param collection 集合名
     * @param elapsedMillis 耗时毫秒
     * @param slow 是否慢调用
     */
    void record(String command, String collection, long elapsedMillis, boolean slow);
}
