package com.peach.common.unique.impl.provider;

import com.peach.common.unique.UniqueGenerator;
import com.peach.common.unique.UniqueGeneratorConst;
import com.peach.common.unique.UniqueGeneratorProvider;
import com.peach.common.unique.impl.SnowflakeUniqueGenerator;

/**
 * 64 位雪花算法（Snowflake）生成器的 SPI 提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
public class SnowflakeProvider implements UniqueGeneratorProvider {

    private final UniqueGenerator generator = new SnowflakeUniqueGenerator();

    @Override
    public String type() {
        return UniqueGeneratorConst.SNOWFLAKE;
    }

    @Override
    public UniqueGenerator getGenerator() {
        return generator;
    }
}
