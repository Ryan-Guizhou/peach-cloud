package com.peach.redis.listener;

import java.io.Serial;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * 多节点缓存通知消息。
 *
 * @param cacheName 缓存名称
 * @param key 缓存键，序列化跨节点传输时会被清空
 * @param sender 发送节点标识
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 16:09
 * @Description 多节点缓存通知消息
 */
public record CacheMessage(String cacheName, Object key, Integer sender) implements Serializable {

    @Serial
    private static final long serialVersionUID = 3322404813031251603L;

    private Object writeReplace() throws ObjectStreamException {
        return new SerializedForm(cacheName, sender);
    }

    private static final class SerializedForm implements Serializable {

        @Serial
        private static final long serialVersionUID = -5261253543605003715L;

        private final String cacheName;
        private final Integer sender;

        private SerializedForm(String cacheName, Integer sender) {
            this.cacheName = cacheName;
            this.sender = sender;
        }

        private Object readResolve() throws ObjectStreamException {
            return new CacheMessage(cacheName, null, sender);
        }
    }
}
