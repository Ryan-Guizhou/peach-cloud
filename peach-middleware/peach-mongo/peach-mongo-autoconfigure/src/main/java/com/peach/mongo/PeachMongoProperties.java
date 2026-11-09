package com.peach.mongo;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;


/**
 * peach.mongo 可配置项，控制连接池、超时、重试、模板行为与事务。
 *
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
     * 命令日志与慢命令。
     */
    private Log log = new Log();

    /**
     * Pool。
     * - maxSize：连接池最大连接数
     * - minSize：连接池最小保留连接数
     * - maxConnecting：并发建立连接的最大数
     * - maxConnectionIdleTimeMs：连接空闲超过该时间将被回收
     * - maintenanceInitialDelayMs：维护任务初始延迟
     * - maintenanceFrequencyMs：维护任务执行频率
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
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
     * Socket。
     * - connectTimeoutMs：建立 TCP 连接的超时
     * - readTimeoutMs：读取数据的超时
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Data
    public static class Socket {
        private int connectTimeoutMs = 10000;
        private int readTimeoutMs = 10000;
    }

    /**
     * Cluster。
     * - serverSelectionTimeoutMs：选择可用服务器节点的最大等待时间
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Data
    public static class Cluster {
        private long serverSelectionTimeoutMs = 10000;
    }

    /**
     * 模板。
     * - removeClassField：是否移除 _class 类型标识字段，减少文档冗余
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Data
    public static class Template {
        private boolean removeClassField = true;
    }

    /**
     * 事务。
     * - enabled：是否开启 MongoTransactionManager（要求副本集/集群）
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Data
    public static class Transaction {
        private boolean enabled = false;
    }

    /**
     * Mongo 命令日志。
     * <p>{@code executable=false} 时只输出把用户数据换成 {@code ?} 的命令和 Parameters。
     * {@code executable=true} 时只输出 {@code db.runCommand(...)}。两种格式不会同时出现。</p>
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/9 16:40
     */
    @Data
    public static class Log {

        /**
         * 是否输出命令日志和慢命令。关闭后不注册命令监听器。
         */
        private boolean enabled = true;

        /**
         * 是否输出可执行命令。
         * <p>false：Preparing 中的用户数据为 {@code ?}，并单独输出 Parameters。
         * true：Preparing 为 {@code db.runCommand(...) }，不再输出 Parameters，也不再输出 {@code ?} 形式。</p>
         */
        private boolean executable = false;

        /**
         * 慢命令阈值，单位毫秒。达到阈值的命令使用 WARN，其余命令使用 DEBUG。
         */
        private int longQueryTime = 3000;

        /**
         * DEBUG 日志中 Preparing 文本的最大长度。慢命令和失败命令不截断，Parameters 行不截断。
         */
        private int maxCommandLength = 4000;

        /**
         * update / delete 过滤条件为空时告警。
         */
        private boolean warnEmptyFilter = true;

        /**
         * 首批返回文档数超过该值时告警。小于等于 0 表示关闭。
         */
        private int largeResultThreshold = 5000;

        /**
         * 不输出的驱动内部命令。
         */
        private List<String> excludeCommands = new ArrayList<>(List.of(
                "ping", "hello", "ismaster", "saslStart", "saslContinue", "endSessions", "buildInfo"));
    }
}
