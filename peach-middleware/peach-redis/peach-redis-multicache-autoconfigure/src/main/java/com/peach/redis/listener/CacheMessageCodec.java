package com.peach.redis.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * 多级缓存 Pub/Sub 消息编解码。
 * <p>缓存同步消息使用固定类型 JSON，不走 RedisTemplate 的多态 {@code @class} 序列化。</p>
 */
public final class CacheMessageCodec {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private CacheMessageCodec() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] serialize(CacheMessage message) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(message);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize cache message", exception);
        }
    }

    public static CacheMessage deserialize(byte[] body) {
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(body, CacheMessage.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to deserialize cache message", exception);
        }
    }
}
