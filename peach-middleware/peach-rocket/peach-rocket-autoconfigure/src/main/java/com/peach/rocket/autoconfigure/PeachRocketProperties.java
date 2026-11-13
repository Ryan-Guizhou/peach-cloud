package com.peach.rocket.autoconfigure;

import com.peach.rocket.exception.MqException;
import lombok.Getter;
import lombok.Setter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Peach RocketMQ 的统一配置契约。
 *
 * <p>嵌套属性使用 Lombok 生成 Spring Binder 所需访问器，保留显式校验、
 * 默认值及集合的空值处理；安全配置不生成包含密钥的 toString。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "peach.rocket", ignoreUnknownFields = false)
public class PeachRocketProperties implements InitializingBean {

    /**
     * 是否启用 Peach RocketMQ 自动配置，默认启用。
     */
    private boolean enabled = true;

    /** 生产严格模式：禁止内存可靠性降级、广播和未证明严格顺序的 Outbox。 */
    private boolean production = false;

    private final Client client = new Client();

    private final Protocol protocol = new Protocol();

    /**
     * 命名空间，通常用于区分环境。
     */
    private String namespace = "default";

    /**
     * 当前应用名称，会写入消息信封。
     */
    private String appName = "application";

    private final Producer producer = new Producer();

    private final Consumer consumer = new Consumer();

    private final Naming naming = new Naming();

    private final Security security = new Security();

    private final Transaction transaction = new Transaction();

    private final Topic topic = new Topic();

    private final Outbox outbox = new Outbox();

    private final Jdbc jdbc = new Jdbc();

    @Override
    public void afterPropertiesSet() {
        requireText(namespace, "peach.rocket.namespace");
        requireText(appName, "peach.rocket.app-name");
        if (!namespace.matches("[A-Za-z0-9_.-]{1,64}") || !appName.matches("[A-Za-z0-9_.-]{1,64}")) {
            throw new MqException("MQ namespace and app-name must be 1-64 ASCII identifier characters");
        }
        requireText(naming.topicPrefix, "peach.rocket.naming.topic-prefix");
        requireText(naming.groupPrefix, "peach.rocket.naming.group-prefix");
        requireText(naming.topicSeparator, "peach.rocket.naming.topic-separator");
        if (!validMillis(producer.defaultTimeout, 60000)) {
            throw new MqException("peach.rocket.producer.default-timeout must be positive");
        }
        if (producer.maxPayloadBytes < 1 || producer.maxHeaderCount < 1 || producer.maxHeaderBytes < 1
                || producer.maxInflight < 1 || producer.inflightAcquireTimeout == null
                || producer.inflightAcquireTimeout.isNegative()) {
            throw new MqException("peach.rocket.producer resource limits are invalid");
        }
        if (consumer.idempotentExpire == null || consumer.idempotentExpire.isNegative() || consumer.idempotentExpire.isZero()) {
            throw new MqException("peach.rocket.consumer.idempotent-expire must be positive");
        }
        if (consumer.consumeThreadMin <= 0 || consumer.consumeThreadMax < consumer.consumeThreadMin
                || consumer.inMemoryMaxEntries < 1 || consumer.maxInflight < 1
                || consumer.maxDynamicConsumers < 1
                || consumer.inflightAcquireTimeout == null || consumer.inflightAcquireTimeout.isNegative()
                || consumer.shutdownDrainTimeout == null || consumer.shutdownDrainTimeout.isNegative()) {
            throw new MqException("peach.rocket.consumer resource limits are invalid");
        }
        if (outbox.enabled && (outbox.batchSize < 1 || outbox.batchSize > 500
                || outbox.scanIntervalMs < 1 || outbox.inMemoryMaxEntries < 1
                || outbox.inMemoryMaxPayloadBytes < 1 || outbox.inMemoryRetention == null
                || outbox.inMemoryRetention.isZero() || outbox.inMemoryRetention.isNegative()
                || outbox.inMemoryLease == null || outbox.inMemoryLease.isZero()
                || outbox.inMemoryLease.isNegative()
                || (outbox.parallelEnabled && !StringUtils.hasText(outbox.executorBeanName)))) {
            throw new MqException("Invalid peach.rocket.outbox resource limits");
        }
        if (topic.readQueueNums <= 0 || topic.writeQueueNums <= 0) {
            throw new MqException("peach.rocket.topic read/write queue nums must be positive");
        }
        if (producer.maxEncodingInflight < 1
                || producer.inflightAcquireTimeout.compareTo(Duration.ofSeconds(30)) > 0
                || consumer.inflightAcquireTimeout.compareTo(Duration.ofSeconds(30)) > 0
                || !validMillis(consumer.shutdownDrainTimeout, 300000)
                || protocol.maxDepth < 1 || protocol.maxDepth > 256
                || protocol.maxStringLength < 1 || protocol.maxNumberLength < 1) {
            throw new MqException("Invalid RocketMQ resource or protocol budget");
        }
        if (!"AES_GCM_AAD".equals(security.algorithm)) {
            throw new MqException("Unsupported MQ encryption algorithm");
        }
        if (StringUtils.hasText(client.accessKey) != StringUtils.hasText(client.secretKey)) {
            throw new MqException("MQ client access-key and secret-key must be configured together");
        }
        if (!Set.of("FIRST_OFFSET", "LAST_OFFSET").contains(consumer.consumeFromWhere)) {
            throw new MqException("MQ consume-from-where must be FIRST_OFFSET or LAST_OFFSET");
        }
        if (!validMillis(jdbc.processingLease, 3600000) || !validMillis(jdbc.outboxLease, 3600000)
                || jdbc.retention == null || jdbc.retention.compareTo(Duration.ofHours(1)) < 0
                || jdbc.outboxLease.toMillis() <= 2 * producer.defaultTimeout.toMillis()) {
            throw new MqException("MQ JDBC leases must cover send timeout; retention must be at least one hour");
        }
        if (production && (!client.tlsEnabled || !StringUtils.hasText(client.accessKey)
                || !consumer.enableIdempotent || topic.autoCreate
                || !naming.autoPrefixConsumerGroup
                || !security.enabled || !security.encryptPayload
                || !"AES_GCM_AAD".equals(security.algorithm)
                || "default".equals(namespace) || "application".equals(appName))) {
            throw new MqException("Production RocketMQ requires explicit identity, ACL/TLS, authenticated encryption, durable idempotency and governed topics");
        }
        if (production && transaction.enabled && "peach-rocket-transaction-producer".equals(transaction.producerGroup)) {
            throw new MqException("Production transaction producer-group must be explicitly scoped to environment and service");
        }
        if (production && (!Boolean.parseBoolean(System.getProperty("tls.client.authServer", "false"))
                || Boolean.parseBoolean(System.getProperty("tls.test.mode.enable", "true"))
                || !Files.isRegularFile(Path.of(System.getProperty("tls.client.trustCertPath", ""))))) {
            throw new MqException("Production requires TLS test mode disabled, server authentication and a readable trust certificate file");
        }
    }

    private boolean validMillis(Duration value, long upperBound) {
        return value != null && !value.isNegative() && value.compareTo(Duration.ofMillis(1)) >= 0
                && value.compareTo(Duration.ofMillis(upperBound)) <= 0;
    }

    private void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new MqException(propertyName + " must not be blank");
        }
    }


    /**
     * 生产者。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Getter
    @Setter
    public static class Producer {
        /** 单实例并发编码/加密预算，避免发送准入之前出现无界序列化。 */
        private int maxEncodingInflight = 64;
        /**
         * 普通发送超时时间。
         */
        private Duration defaultTimeout = Duration.ofSeconds(3);

        /**
         * 已编码消息信封允许的最大字节数；默认 4 MiB。
         */
        private int maxPayloadBytes = 4 * 1024 * 1024;

        /**
         * 消息头最大项数，包括业务 Key 属性。
         */
        private int maxHeaderCount = 64;

        /**
         * 消息头 Key/Value UTF-8 总字节上限。
         */
        private int maxHeaderBytes = 64 * 1024;

        /**
         * 单个 Producer 实例的同步、异步和单向发送并发总上限。
         */
        private int maxInflight = 1024;

        /**
         * 获取发送额度允许等待的最长时间。
         */
        private Duration inflightAcquireTimeout = Duration.ofMillis(200);
    }

    /**
     * 消费者。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Getter
    @Setter
    public static class Consumer {
        /** 新消费组首次启动的位置，不影响已经存在的位点。 */
        private String consumeFromWhere = "LAST_OFFSET";
        /** 是否启用消费幂等；默认启用，内存实现仅适用于单 JVM。 */
        private boolean enableIdempotent = true;
        /** 幂等记录有效期；默认 24 小时。 */
        private Duration idempotentExpire = Duration.ofHours(24);
        /** 是否扫描并启动带注解的动态 Consumer。 */
        private boolean dynamicRegister = true;
        /** RocketMQ SDK 单 Consumer 的最小工作线程数。 */
        private int consumeThreadMin = 1;
        /** RocketMQ SDK 单 Consumer 的最大工作线程数。 */
        private int consumeThreadMax = 20;
        /** 本地消费幂等缓存条目上限。 */
        private int inMemoryMaxEntries = 32768;
        /** 生产配置可要求使用具备 Owner Token 的持久化幂等 Store。 */
        private boolean requireDurableIdempotency = false;
        /** 同一应用允许注册的最大动态 Consumer 数量。 */
        private int maxDynamicConsumers = 64;
        /** 动态 Consumer 共享的本地业务 Handler 最大并发数。 */
        private int maxInflight = 128;
        /** 等待业务执行额度的最长时间。 */
        private Duration inflightAcquireTimeout = Duration.ofMillis(200);
        /** 停止 Consumer 前等待在途业务排空的最长时间。 */
        private Duration shutdownDrainTimeout = Duration.ofSeconds(30);
    }

    /**
     * Naming。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Getter
    @Setter
    public static class Naming {
        /** 业务 Topic 前缀。 */
        private String topicPrefix = "biz";
        /** 可选 Consumer Group 前缀。 */
        private String groupPrefix = "cg";
        /** 环境与前缀之间的分隔符。 */
        private String topicSeparator = "-";
        /** 是否自动为 Topic 增加环境前缀。 */
        private boolean autoPrefixEnv = true;
        /** 是否为 Consumer Group 增加环境前缀；新项目默认开启环境隔离。 */
        private boolean autoPrefixConsumerGroup = true;
    }

    /**
     * 安全。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Getter
    @Setter
    public static class Security {
        /** 是否启用当前子功能，默认关闭。 */
        private boolean enabled = false;
        /** 是否对所有业务载荷启用加密。 */
        private boolean encryptPayload = false;
        /** 消息加密算法标识。 */
        private String algorithm = "AES_GCM_AAD";
        /** 新消息使用的活动密钥 ID。 */
        private String keyId = "default";
        /** 按 keyId 配置的密钥映射，支持历史消息密钥轮换。 */
        private Map<String, String> keys = new LinkedHashMap<>();
        /** 是否按 Base64 解码配置的密钥。 */
        private boolean base64Key = false;
        /** 需要加密的 Topic 列表。 */
        private List<String> encryptTopics = new ArrayList<String>();
        public void setKeys(Map<String, String> keys) {
            this.keys = keys == null ? new LinkedHashMap<>() : new LinkedHashMap<>(keys);
        }

        public void setEncryptTopics(List<String> encryptTopics) {
            this.encryptTopics = encryptTopics == null ? new ArrayList<>() : encryptTopics;
        }
    }

    /**
     * 事务。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Getter
    @Setter
    public static class Transaction {
        /** 是否启用当前子功能。 */
        private boolean enabled = true;
        /** 事务 Producer Group，需与 Broker 配置匹配。 */
        private String producerGroup = "peach-rocket-transaction-producer";
    }

    /**
     * Topic。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Getter
    @Setter
    public static class Topic {
        /** 是否允许在应用启动时尝试创建 Topic；生产推荐关闭。 */
        private boolean autoCreate = false;
        /** Topic 默认读队列数量。 */
        private int readQueueNums = 4;
        /** Topic 默认写队列数量。 */
        private int writeQueueNums = 4;
        /** 是否自动发现消费者声明的 Topic。 */
        private boolean includeConsumerTopics = true;
        /** 是否自动发现事务处理器声明的 Topic。 */
        private boolean includeTransactionTopics = true;
        /** 额外显式声明的 Topic。 */
        private List<String> topics = new ArrayList<String>();
        public void setTopics(List<String> topics) {
            this.topics = topics == null ? new ArrayList<>() : topics;
        }
    }

    /**
     * JDBC 持久化存储模块（需显式引入 peach-rocket-jdbc）。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/12
     */
    @Getter
    @Setter
    public static class Jdbc {
        /** JDBC 消费处理租约；与成功记录保留期分开。 */
        private Duration processingLease = Duration.ofMinutes(5);
        /** JDBC Outbox 认领租约；必须大于发送超时的两倍。 */
        private Duration outboxLease = Duration.ofMinutes(5);
        /** 已发送 Outbox 终态最短保留期；业务重放窗口不得短于此值。 */
        private Duration retention = Duration.ofDays(7);
        /** 是否启用当前子功能，默认关闭。 */
        private boolean enabled = false;
    }

    /**
     * 发件箱。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    @Getter
    @Setter
    public static class Outbox {
        /** 发布时要求当前 JDBC 资源已加入业务事务；production 会强制要求。 */
        private boolean requireTransaction = false;
        /** 是否启用当前子功能，默认关闭。 */
        private boolean enabled = false;
        /** Outbox 每轮最多领取的消息数。 */
        private int batchSize = 50;
        /** Outbox 扫描间隔，单位毫秒。 */
        private long scanIntervalMs = 2000L;
        /** 是否强制要求持久化 Outbox Store。 */
        private boolean requireDurable = false;
        /** 本地 Outbox 最大记录数。 */
        private int inMemoryMaxEntries = 1024;
        /** 本地 Outbox 单条消息最大字节数。 */
        private int inMemoryMaxPayloadBytes = 65536;
        /** 本地 Outbox 已完成记录的保留时间。 */
        private Duration inMemoryRetention = Duration.ofHours(24);
        /** 本地 Outbox 认领租约时间。 */
        private Duration inMemoryLease = Duration.ofMinutes(5);
        /** 是否使用已配置的受管执行器并行投递普通 Outbox 消息。 */
        private boolean parallelEnabled = false;
        /** 并行投递绑定的受管 ExecutorService Bean 名称。 */
        private String executorBeanName = "rocket-outboxVirtualExecutor";
    }

    /**
     * 独立 SDK 客户端的统一安全配置；凭据不得通过 toString 输出。
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/13 15:00
     */
    @Getter
    @Setter
    public static class Client {
        /** 业务最小权限访问标识，通过安全配置源注入。 */
        private String accessKey = "";
        /** 访问密钥，通过安全配置源注入，禁止进入日志。 */
        private String secretKey = "";
        /** 是否要求 SDK 使用 TLS；生产必须启用。 */
        private boolean tlsEnabled = false;
    }

    /**
     * JSON 协议解析预算，不允许消息覆盖本地限制。
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/13 15:00
     */
    @Getter
    @Setter
    public static class Protocol {
        /** 最大 JSON 嵌套深度。 */
        private int maxDepth = 64;
        /** 单个字符串最大字符数。 */
        private int maxStringLength = 1048576;
        /** 单个数字最大字符数。 */
        private int maxNumberLength = 128;
    }
}
