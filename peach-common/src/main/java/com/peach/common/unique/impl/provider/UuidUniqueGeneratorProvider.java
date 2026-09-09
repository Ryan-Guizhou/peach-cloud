package com.peach.common.unique.impl.provider;

import com.peach.common.unique.UniqueGenerator;
import com.peach.common.unique.UniqueGeneratorConst;
import com.peach.common.unique.UniqueGeneratorProvider;
import com.peach.common.unique.impl.UuidUniqueGenerator;

/**
 * 32 位无短横线 UUID 生成器的 SPI 提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
public class UuidUniqueGeneratorProvider implements UniqueGeneratorProvider {

    private final UniqueGenerator generator = new UuidUniqueGenerator();

    @Override
    public String type() {
        return UniqueGeneratorConst.UUID;
    }

    @Override
    public UniqueGenerator getGenerator() {
        return generator;
    }
}
