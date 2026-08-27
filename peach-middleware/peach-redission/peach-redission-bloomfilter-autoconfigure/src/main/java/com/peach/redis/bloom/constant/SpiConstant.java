package com.peach.redis.bloom.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/8 14:08
 */
public final class SpiConstant {

    private SpiConstant() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * SPI 名称常量
     */
    public static final String SCALE_POLICY_SPI_NAME = "BloomScalePolicy";

    public static final String CODEC_SPI_NAME = "CodecProvider";

    public static final String KEY_NAMING_SPI_NAME = "KeyNamingStrategy";
}
