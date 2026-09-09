package com.peach.common.unique.impl.provider;

import com.peach.common.unique.UniqueGenerator;
import com.peach.common.unique.UniqueGeneratorConst;
import com.peach.common.unique.UniqueGeneratorProvider;
import com.peach.common.unique.impl.NanoUniqueGenerator;

/**
 * NanoID 生成器的 SPI 提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
public class NanoUniqueProvider implements UniqueGeneratorProvider {

    private final UniqueGenerator generator = new NanoUniqueGenerator();

    @Override
    public String type() {
        return UniqueGeneratorConst.NANOID;
    }

    @Override
    public UniqueGenerator getGenerator() {
        return generator;
    }
}
