package com.peach.common.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 项目级 CSPRNG 单例，供 token 种子、验证码、加密 IV 等场景复用。
 */
public final class PeachSecureRandom {

    private static final SecureRandom INSTANCE = createInstance();

    private PeachSecureRandom() {
        throw new IllegalStateException("Utility class");
    }

    private static SecureRandom createInstance() {
        try {
            return SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException ex) {
            return new SecureRandom();
        }
    }

    /**
     * 返回共享 {@link SecureRandom} 实例。
     */
    public static SecureRandom get() {
        return INSTANCE;
    }
}
