package com.peach.common.unique.impl;

import com.peach.common.unique.UniqueGenerator;
import com.peach.common.unique.UniqueGeneratorConst;
import com.peach.common.util.PeachSecureRandom;

import java.security.SecureRandom;

/**
 * 紧凑型 URL 安全 NanoID 生成器实现。
 *
 * <p>特性说明：
 * <ul>
 *   <li>字符集：采用 64 个 URL 安全字符（{@code 0-9a-zA-Z_-}），无任何特殊符号或空白。</li>
 *   <li>位掩码抽取：由于字符集大小刚好为 \(2^6 = 64\)，采用按位与掩码 {@code & 0x3F} 进行无偏差直接抽取，性能优异。</li>
 *   <li>默认长度：21 位字符，在 1 秒生成 1000 个 ID 的场景下，需要约 4100 万年才有 1% 的碰撞概率。</li>
 *   <li>随机源：基于 {@link PeachSecureRandom} 强安全随机数生成器。</li>
 * </ul>
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
public class NanoUniqueGenerator implements UniqueGenerator {

    /**
     * 默认 NanoID 字符集（64 字符，2^6=64，可无偏差直接位与掩码抽取）。
     */
    private static final char[] DEFAULT_ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_".toCharArray();

    /**
     * 默认生成长度（21 字符）。
     */
    public static final int DEFAULT_SIZE = 21;

    private static final int MASK = 0x3F;

    private final int size;

    private final SecureRandom random;

    /**
     * 使用默认 21 位长度及全局安全随机源构造生成器。
     */
    public NanoUniqueGenerator() {
        this(DEFAULT_SIZE, PeachSecureRandom.get());
    }

    /**
     * 指定生成长度构造生成器。
     *
     * @param size 生成的字符串长度，必须大于 0
     * @throws IllegalArgumentException 当 size &le; 0 时抛出
     */
    public NanoUniqueGenerator(int size) {
        this(size, PeachSecureRandom.get());
    }

    /**
     * 指定生成长度及安全随机源构造生成器。
     *
     * @param size   生成的字符串长度，必须大于 0
     * @param random 安全随机数生成器，若为 null 则自动使用 {@link PeachSecureRandom#get()}
     * @throws IllegalArgumentException 当 size &le; 0 时抛出
     */
    public NanoUniqueGenerator(int size, SecureRandom random) {
        if (size <= 0) {
            throw new IllegalArgumentException("NanoID size must be greater than zero");
        }
        this.size = size;
        this.random = random != null ? random : PeachSecureRandom.get();
    }

    /**
     * 生成下一个 21 位 NanoID 字符串。
     *
     * @return 21 位 URL 安全字符串
     */
    @Override
    public String nextId() {
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        char[] chars = new char[size];
        for (int i = 0; i < size; i++) {
            chars[i] = DEFAULT_ALPHABET[bytes[i] & MASK];
        }
        return new String(chars);
    }

    /**
     * 算法标识类型。
     *
     * @return {@link UniqueGeneratorConst#NANOID} ("nanoid")
     */
    @Override
    public String type() {
        return UniqueGeneratorConst.NANOID;
    }

    /**
     * 获取当前生成器配置的 ID 长度。
     *
     * @return 长度大小
     */
    public int getSize() {
        return size;
    }
}
