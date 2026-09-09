package com.peach.common.unique.impl;

import com.peach.common.unique.UniqueGenerator;
import com.peach.common.unique.UniqueGeneratorConst;

import java.util.UUID;

/**
 * 32 位无短横线 UUID 生成器实现。
 *
 * <p>基于 {@link UUID#randomUUID()} 生成标准 UUID 并移除 {@code "-"} 分隔符，
 * 输出 32 字符的纯十六进制小写字符串。作为历史存量逻辑的无缝兼容实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
public class UuidUniqueGenerator implements UniqueGenerator {

    private static final String SEPARATOR = "-";

    private static final String EMPTY = "";

    /**
     * 生成 32 位无横杠 UUID 字符串。
     *
     * @return 32 位十六进制字符串
     */
    @Override
    public String nextId() {
        return UUID.randomUUID().toString().replace(SEPARATOR, EMPTY);
    }

    /**
     * 算法标识类型。
     *
     * @return {@link UniqueGeneratorConst#UUID} ("uuid")
     */
    @Override
    public String type() {
        return UniqueGeneratorConst.UUID;
    }
}
