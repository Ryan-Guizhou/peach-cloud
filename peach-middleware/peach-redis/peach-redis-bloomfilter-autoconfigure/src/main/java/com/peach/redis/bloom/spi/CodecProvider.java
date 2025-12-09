package com.peach.redis.bloom.spi;

import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;

/**
 * 编解码器提供者 SPI：用于配置 Redisson 的 {@link org.redisson.client.codec.Codec}。
 * 默认提供单例的 Jackson JSON 与字符串编解码器实现。
 */
public interface CodecProvider {
    Codec codec();

    String getName();

    static CodecProvider defaultJacksonCodec() {
        return new CodecProvider() {
            private final Codec singleton = new JsonJacksonCodec();
            @Override
            public Codec codec() { return singleton; }

            @Override
            public String getName() { return "json-jackson"; }
        };
    }
}