package com.peach.rocket.core;

/**
 * MQ消息Model枚举。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
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
