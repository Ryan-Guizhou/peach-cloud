package com.peach.common.unique;

/**
 * 统一 ID 生成器接口。
 *
 * <p>定义所有 ID 生成算法的通用规范，包含字符串 ID 生成、长整型数值 ID 生成以及算法标识能力。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
public interface UniqueGenerator {

    /**
     * 生成下一个全局唯一字符串 ID。
     *
     * @return 非空字符串 ID（格式与长度取决于具体实现）
     */
    String nextId();

    /**
     * 生成下一个 64 位长整型正整数 ID。
     *
     * <p>仅数值型生成器（例如 {@code snowflake}）支持此操作；
     * 非数值型生成器（例如 {@code nanoid}、{@code uuid}）调用将抛出异常。</p>
     *
     * @return 64 位长整型 ID
     * @throws UnsupportedOperationException 当当前算法不支持生成纯数字 ID 时抛出
     */
    default long nextLongId() {
        throw new UnsupportedOperationException("Numeric ID is not supported by generator: " + type());
    }

    /**
     * 获取当前生成器的算法类型标识。
     *
     * @return 算法类型标识字符串，如 {@link UniqueGeneratorConst#UUID}、{@link UniqueGeneratorConst#NANOID}、{@link UniqueGeneratorConst#SNOWFLAKE}
     */
    String type();
}
