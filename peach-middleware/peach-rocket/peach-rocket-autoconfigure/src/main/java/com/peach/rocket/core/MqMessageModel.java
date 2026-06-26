package com.peach.rocket.core;

/**
 * MQ 消息模型。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
public enum MqMessageModel {

    /**
     * 集群消费，同组内只有一个实例消费消息。
     */
    CLUSTERING,

    /**
     * 广播消费，同组内每个实例都会消费消息。
     */
    BROADCASTING
}
