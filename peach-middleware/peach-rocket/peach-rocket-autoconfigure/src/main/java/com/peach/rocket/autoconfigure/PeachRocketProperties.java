package com.peach.rocket.autoconfigure;

import com.peach.rocket.exception.MqException;
import lombok.Getter;
import lombok.Setter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
@ConfigurationProperties(prefix = "peach.rocket")
public class PeachRocketProperties implements InitializingBean {

    /**
     * 是否启用 Peach RocketMQ 自动配置，默认启用。
     */
    private boolean enabled = true;

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
        requireText(naming.topicPrefix, "peach.rocket.naming.topic-prefix");
        requireText(naming.groupPrefix, "peach.rocket.naming.group-prefix");
        requireText(naming.topicSeparator, "peach.rocket.naming.topic-separator");
        if (producer.defaultTimeout == null || producer.defaultTimeout.isNegative() || producer.defaultTimeout.isZero()) {
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
        private boolean enableIdempotent = true;
        private Duration idempotentExpire = Duration.ofHours(24);
        private boolean dynamicRegister = true;
        private int consumeThreadMin = 1;
        private int consumeThreadMax = 20;
        private int inMemoryMaxEntries = 32768;
        private boolean requireDurableIdempotency = false;
        private int maxDynamicConsumers = 64;
        private int maxInflight = 128;
        private Duration inflightAcquireTimeout = Duration.ofMillis(200);
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
        private String topicPrefix = "biz";
        private String groupPrefix = "cg";
        private String topicSeparator = "-";
        private boolean autoPrefixEnv = true;
        private boolean autoPrefixConsumerGroup = false;
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
        private boolean enabled = false;
        private boolean encryptPayload = false;
        private String algorithm = "AES_GCM";
        private String keyId = "default";
        private String key = "";
        private Map<String, String> keys = new LinkedHashMap<>();
        private boolean base64Key = false;
        private List<String> encryptTopics = new ArrayList<String>();
        public void setKeys(Map<String, String> keys) {
            this.keys = keys == null ? new LinkedHashMap<>() : new LinkedHashMap<>(keys);
        }
        public void setEncryptTopics(List<String> encryptTopics) { this.encryptTopics = encryptTopics == null ? new ArrayList<String>() : encryptTopics; }
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
        private boolean enabled = true;
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
        private boolean autoCreate = false;
        private int readQueueNums = 4;
        private int writeQueueNums = 4;
        private boolean includeConsumerTopics = true;
        private boolean includeTransactionTopics = true;
        private List<String> topics = new ArrayList<String>();
        public void setTopics(List<String> topics) { this.topics = topics == null ? new ArrayList<String>() : topics; }
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
        private boolean enabled = false;
        private int batchSize = 50;
        private long scanIntervalMs = 2000L;
        private boolean requireDurable = false;
        private int inMemoryMaxEntries = 1024;
        private int inMemoryMaxPayloadBytes = 65536;
        private Duration inMemoryRetention = Duration.ofHours(24);
        private Duration inMemoryLease = Duration.ofMinutes(5);
        private boolean parallelEnabled = false;
        private String executorBeanName = "rocket-outboxVirtualExecutor";
    }
}
