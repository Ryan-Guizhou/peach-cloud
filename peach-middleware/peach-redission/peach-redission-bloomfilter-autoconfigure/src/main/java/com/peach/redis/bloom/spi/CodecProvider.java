package com.peach.redis.bloom.spi;

import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;

/**
 * Codec提供者。
 * 默认提供单例的 Jackson JSON 与字符串编解码器实现。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public interface CodecProvider {
    Codec codec();

    String getName();

    static CodecProvider defaultJacksonCodec() {
        return DefaultJacksonCodecProvider.INSTANCE;
    }

    /**
     * 默认JacksonCodec提供者。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    final class DefaultJacksonCodecProvider implements CodecProvider {

        private static final DefaultJacksonCodecProvider INSTANCE = new DefaultJacksonCodecProvider();
        private final Codec singleton = new JsonJacksonCodec();

        private DefaultJacksonCodecProvider() {
        }

        @Override
        public Codec codec() {
            return singleton;
        }

        @Override
        public String getName() {
            return "json-jackson";
        }
    }
}
