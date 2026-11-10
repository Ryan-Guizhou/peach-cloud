package com.peach.redis.listener;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缓存消息SerializationTest。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

class CacheMessageSerializationTest {

    @Test
    void shouldOmitKeyAfterJavaSerializationRoundTrip() throws Exception {
        CacheMessage original = new CacheMessage("userCache", "user-42", 3);

        byte[] bytes;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
            objectOutputStream.writeObject(original);
            bytes = outputStream.toByteArray();
        }

        CacheMessage restored;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            restored = (CacheMessage) objectInputStream.readObject();
        }

        assertThat(restored.cacheName()).isEqualTo("userCache");
        assertThat(restored.sender()).isEqualTo(3);
        assertThat(restored.key()).isNull();
    }

    @Test
    void jsonPubSubMustNotExposeBusinessKeyAndInvalidatesEntireCache() {
        String sensitive = "private:user@example.invalid";
        CacheMessage message = new CacheMessage("products", sensitive, 100);
        byte[] json = CacheMessageCodec.serialize(message);

        assertThat(new String(json, java.nio.charset.StandardCharsets.UTF_8))
                .doesNotContain(sensitive);
        CacheMessage restored = CacheMessageCodec.deserialize(json);
        assertThat(restored.cacheName()).isEqualTo("products");
        assertThat(restored.key()).isNull();
        assertThat(restored.sender()).isEqualTo(100);
    }

}
