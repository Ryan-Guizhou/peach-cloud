package com.peach.mongo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * peach.mongo 可配置项，控制连接池、超时、重试、模板行为与事务。
 */

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/13 17:01
 * @Description peach.mongo 可配置项，控制连接池、超时、重试、模板行为与事务。
 */
@Data
@ConfigurationProperties(prefix = "peach.mongo")
public class PeachMongoProperties {

    private String uri;

    private String database;

    /**
     * 连接池相关配置
     */
    private Pool pool = new Pool();
    /**
     * Socket 连接与读取超时配置（毫秒）
     */
    private Socket socket = new Socket();
    /**
     * 集群选择服务器超时（毫秒）
     */
    private Cluster cluster = new Cluster();
    /**
     * MongoTemplate 行为配置
     */
    private Template template = new Template();
    /**
     * 读操作失败是否自动重试（驱动层）
     */
    private boolean retryReads = true;
    /**
     * 写操作失败是否自动重试（驱动层）
     */
    private boolean retryWrites = true;
    /**
     * 事务相关配置
     */
    private Transaction transaction = new Transaction();

    /**
     * 连接池参数：并发连接与维护策略
     * - maxSize：连接池最大连接数
     * - minSize：连接池最小保留连接数
     * - maxConnecting：并发建立连接的最大数
     * - maxConnectionIdleTimeMs：连接空闲超过该时间将被回收
     * - maintenanceInitialDelayMs：维护任务初始延迟
     * - maintenanceFrequencyMs：维护任务执行频率
     */
    @Data
    public static class Pool {
        private int maxSize = 100;
        private int minSize = 5;
        private int maxConnecting = 20;
        private long maxConnectionIdleTimeMs = 10000;
        private long maintenanceInitialDelayMs = 30000;
        private long maintenanceFrequencyMs = 10000;
    }

    /**
     * Socket 超时配置
     * - connectTimeoutMs：建立 TCP 连接的超时
     * - readTimeoutMs：读取数据的超时
     */
    @Data
    public static class Socket {
        private int connectTimeoutMs = 10000;
        private int readTimeoutMs = 10000;
    }

    /**
     * 集群选择超时配置
     * - serverSelectionTimeoutMs：选择可用服务器节点的最大等待时间
     */
    @Data
    public static class Cluster {
        private long serverSelectionTimeoutMs = 10000;
    }

    /**
     * 模板行为配置
     * - removeClassField：是否移除 _class 类型标识字段，减少文档冗余
     */
    @Data
    public static class Template {
        private boolean removeClassField = true;
    }

    /**
     * 事务配置
     * - enabled：是否开启 MongoTransactionManager（要求副本集/集群）
     */
    @Data
    public static class Transaction {
        private boolean enabled = false;
    }
}
