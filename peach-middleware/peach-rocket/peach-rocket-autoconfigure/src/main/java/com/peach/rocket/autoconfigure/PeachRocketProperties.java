package com.peach.rocket.autoconfigure;

import com.peach.rocket.exception.MqException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * PeachRocketMQ配置属性。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
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

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public Producer getProducer() { return producer; }
    public Consumer getConsumer() { return consumer; }
    public Naming getNaming() { return naming; }
    public Security getSecurity() { return security; }
    public Transaction getTransaction() { return transaction; }
    public Topic getTopic() { return topic; }
    public Outbox getOutbox() { return outbox; }
    public Jdbc getJdbc() { return jdbc; }

    /**
     * 生产者。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    public static class Producer {
        private Duration defaultTimeout = Duration.ofSeconds(3);
        public Duration getDefaultTimeout() { return defaultTimeout; }
        public void setDefaultTimeout(Duration defaultTimeout) { this.defaultTimeout = defaultTimeout; }
    }

    /**
     * 消费者。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
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
        public boolean isEnableIdempotent() { return enableIdempotent; }
        public void setEnableIdempotent(boolean enableIdempotent) { this.enableIdempotent = enableIdempotent; }
        public Duration getIdempotentExpire() { return idempotentExpire; }
        public void setIdempotentExpire(Duration idempotentExpire) { this.idempotentExpire = idempotentExpire; }
        public boolean isDynamicRegister() { return dynamicRegister; }
        public void setDynamicRegister(boolean dynamicRegister) { this.dynamicRegister = dynamicRegister; }
        public int getConsumeThreadMin() { return consumeThreadMin; }
        public void setConsumeThreadMin(int consumeThreadMin) { this.consumeThreadMin = consumeThreadMin; }
        public int getConsumeThreadMax() { return consumeThreadMax; }
        public void setConsumeThreadMax(int consumeThreadMax) { this.consumeThreadMax = consumeThreadMax; }
        public int getInMemoryMaxEntries() { return inMemoryMaxEntries; }
        public void setInMemoryMaxEntries(int inMemoryMaxEntries) { this.inMemoryMaxEntries = inMemoryMaxEntries; }
        public int getMaxDynamicConsumers() { return maxDynamicConsumers; }
        public void setMaxDynamicConsumers(int maxDynamicConsumers) { this.maxDynamicConsumers = maxDynamicConsumers; }
        public int getMaxInflight() { return maxInflight; }
        public void setMaxInflight(int maxInflight) { this.maxInflight = maxInflight; }
        public Duration getInflightAcquireTimeout() { return inflightAcquireTimeout; }
        public void setInflightAcquireTimeout(Duration inflightAcquireTimeout) {
            this.inflightAcquireTimeout = inflightAcquireTimeout;
        }
        public Duration getShutdownDrainTimeout() { return shutdownDrainTimeout; }
        public void setShutdownDrainTimeout(Duration shutdownDrainTimeout) {
            this.shutdownDrainTimeout = shutdownDrainTimeout;
        }
        public boolean isRequireDurableIdempotency() { return requireDurableIdempotency; }
        public void setRequireDurableIdempotency(boolean requireDurableIdempotency) {
            this.requireDurableIdempotency = requireDurableIdempotency;
        }
    }

    /**
     * Naming。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    public static class Naming {
        private String topicPrefix = "biz";
        private String groupPrefix = "cg";
        private String topicSeparator = "-";
        private boolean autoPrefixEnv = true;
        public String getTopicPrefix() { return topicPrefix; }
        public void setTopicPrefix(String topicPrefix) { this.topicPrefix = topicPrefix; }
        public String getGroupPrefix() { return groupPrefix; }
        public void setGroupPrefix(String groupPrefix) { this.groupPrefix = groupPrefix; }
        public String getTopicSeparator() { return topicSeparator; }
        public void setTopicSeparator(String topicSeparator) { this.topicSeparator = topicSeparator; }
        public boolean isAutoPrefixEnv() { return autoPrefixEnv; }
        public void setAutoPrefixEnv(boolean autoPrefixEnv) { this.autoPrefixEnv = autoPrefixEnv; }
    }

    /**
     * 安全。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    public static class Security {
        private boolean enabled = false;
        private boolean encryptPayload = false;
        private String algorithm = "AES_GCM";
        private String keyId = "default";
        private String key = "";
        private boolean base64Key = false;
        private List<String> encryptTopics = new ArrayList<String>();
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isEncryptPayload() { return encryptPayload; }
        public void setEncryptPayload(boolean encryptPayload) { this.encryptPayload = encryptPayload; }
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public boolean isBase64Key() { return base64Key; }
        public void setBase64Key(boolean base64Key) { this.base64Key = base64Key; }
        public List<String> getEncryptTopics() { return encryptTopics; }
        public void setEncryptTopics(List<String> encryptTopics) { this.encryptTopics = encryptTopics == null ? new ArrayList<String>() : encryptTopics; }
    }

    /**
     * 事务。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    public static class Transaction {
        private boolean enabled = true;
        private String producerGroup = "peach-rocket-transaction-producer";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getProducerGroup() { return producerGroup; }
        public void setProducerGroup(String producerGroup) { this.producerGroup = producerGroup; }
    }

    /**
     * Topic。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
    public static class Topic {
        private boolean autoCreate = false;
        private int readQueueNums = 4;
        private int writeQueueNums = 4;
        private boolean includeConsumerTopics = true;
        private boolean includeTransactionTopics = true;
        private List<String> topics = new ArrayList<String>();
        public boolean isAutoCreate() { return autoCreate; }
        public void setAutoCreate(boolean autoCreate) { this.autoCreate = autoCreate; }
        public int getReadQueueNums() { return readQueueNums; }
        public void setReadQueueNums(int readQueueNums) { this.readQueueNums = readQueueNums; }
        public int getWriteQueueNums() { return writeQueueNums; }
        public void setWriteQueueNums(int writeQueueNums) { this.writeQueueNums = writeQueueNums; }
        public boolean isIncludeConsumerTopics() { return includeConsumerTopics; }
        public void setIncludeConsumerTopics(boolean includeConsumerTopics) { this.includeConsumerTopics = includeConsumerTopics; }
        public boolean isIncludeTransactionTopics() { return includeTransactionTopics; }
        public void setIncludeTransactionTopics(boolean includeTransactionTopics) { this.includeTransactionTopics = includeTransactionTopics; }
        public List<String> getTopics() { return topics; }
        public void setTopics(List<String> topics) { this.topics = topics == null ? new ArrayList<String>() : topics; }
    }

    /**
     * JDBC 持久化存储模块（需显式引入 peach-rocket-jdbc）。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/12
     */
    public static class Jdbc {
        private boolean enabled = false;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    /**
     * 发件箱。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */
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
        public boolean isParallelEnabled() { return parallelEnabled; }
        public void setParallelEnabled(boolean parallelEnabled) { this.parallelEnabled = parallelEnabled; }
        public String getExecutorBeanName() { return executorBeanName; }
        public void setExecutorBeanName(String executorBeanName) { this.executorBeanName = executorBeanName; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public long getScanIntervalMs() { return scanIntervalMs; }
        public void setScanIntervalMs(long scanIntervalMs) { this.scanIntervalMs = scanIntervalMs; }
        public boolean isRequireDurable() { return requireDurable; }
        public void setRequireDurable(boolean requireDurable) { this.requireDurable = requireDurable; }
        public int getInMemoryMaxEntries() { return inMemoryMaxEntries; }
        public void setInMemoryMaxEntries(int inMemoryMaxEntries) { this.inMemoryMaxEntries = inMemoryMaxEntries; }
        public int getInMemoryMaxPayloadBytes() { return inMemoryMaxPayloadBytes; }
        public void setInMemoryMaxPayloadBytes(int inMemoryMaxPayloadBytes) { this.inMemoryMaxPayloadBytes = inMemoryMaxPayloadBytes; }
        public Duration getInMemoryRetention() { return inMemoryRetention; }
        public void setInMemoryRetention(Duration inMemoryRetention) { this.inMemoryRetention = inMemoryRetention; }
        public Duration getInMemoryLease() { return inMemoryLease; }
        public void setInMemoryLease(Duration inMemoryLease) { this.inMemoryLease = inMemoryLease; }
    }
}
