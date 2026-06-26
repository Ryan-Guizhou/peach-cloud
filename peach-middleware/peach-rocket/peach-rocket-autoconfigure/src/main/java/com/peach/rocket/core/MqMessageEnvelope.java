package com.peach.rocket.core;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MQ 标准消息信封。
 *
 * <p>所有发送到 RocketMQ 的业务消息都会先包装为统一信封，信封中包含路由信息、来源应用、消息版本、
 * 透传头、加密元信息以及实际业务 payload。消费端会基于同一模型完成解码、解密和业务分发。
 *
 * @param <T> payload 类型
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public class MqMessageEnvelope<T> {

    /**
     * 业务侧生成的消息唯一标识。
     */
    private String messageId;

    /**
     * 当前消息 topic。
     */
    private String topic;

    /**
     * 当前消息 tag，可以为空。
     */
    private String tag;

    /**
     * 当前消息业务 key，可以为空。
     */
    private String key;

    /**
     * 发送该消息的应用标识。
     */
    private String producerApp;

    /**
     * payload 的 Java 类型名称。
     */
    private String payloadType;

    /**
     * 业务事件版本号。
     */
    private int version;

    /**
     * 业务透传头信息。
     */
    private Map<String, String> headers = new LinkedHashMap<String, String>();

    /**
     * 实际业务负载。
     */
    private T payload;

    /**
     * 消息信封创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 是否已对 payload 执行加密，true 表示已加密。
     */
    private boolean encrypted;

    /**
     * 当前 payload 使用的加密算法。
     */
    private String encryptionAlgorithm;

    /**
     * 当前 payload 使用的密钥标识。
     */
    private String encryptionKeyId;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getProducerApp() {
        return producerApp;
    }

    public void setProducerApp(String producerApp) {
        this.producerApp = producerApp;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(String payloadType) {
        this.payloadType = payloadType;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<String, String>() : headers;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public String getEncryptionKeyId() {
        return encryptionKeyId;
    }

    public void setEncryptionKeyId(String encryptionKeyId) {
        this.encryptionKeyId = encryptionKeyId;
    }
}
