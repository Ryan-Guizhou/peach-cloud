package com.peach.common.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Peach安全随机数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
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
