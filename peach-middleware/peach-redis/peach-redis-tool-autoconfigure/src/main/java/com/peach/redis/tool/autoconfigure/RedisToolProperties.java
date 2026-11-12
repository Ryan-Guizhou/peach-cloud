package com.peach.redis.tool.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 工具配置，绑定 peach.redis.tool 命名空间。
 *
 * <p>扫描数量和遍历时间预算同时约束 key、Hash、Set 和 ZSet 扫描。
 * 超出任一预算会明确失败，不向调用方返回截断结果。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 16:00
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "peach.redis.tool")
public class RedisToolProperties {

    /** 扫描保护配置；非法值在默认 DAO 构造时拒绝。 */
    private Scan scan = new Scan();

    /**
     * 单次扫描预算；Cluster 的全部主节点共享同一次键扫描预算。
     *
     * <p>结果数量按去重后的键或成员计算。遍历时间在本地检查，
     * 不能中断已阻塞的驱动调用，也不替代连接或命令超时。</p>
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/12 16:00
     */
    @Getter
    @Setter
    public static class Scan {
        /** 最多收集的唯一结果数，默认 10000，有效范围 1–100000。 */
        private int maxResults = 10_000;

        /** 单次遍历时间预算，单位毫秒，默认 10000，有效范围 1–60000。 */
        private long timeoutMillis = 10_000L;
    }
}
