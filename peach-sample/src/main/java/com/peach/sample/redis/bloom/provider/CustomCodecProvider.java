package com.peach.sample.redis.bloom.provider;


import com.peach.redis.bloom.spi.CodecProvider;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/11/27 14:39
 */
public class CustomCodecProvider implements CodecProvider {

    private final Codec singleton = new StringCodec();

    @Override
    public Codec codec() {
        return singleton;
    }

    @Override
    public String getName() {
        return "string-codec";
    }
}
