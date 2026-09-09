package com.peach.common.util;

import lombok.extern.slf4j.Slf4j;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Peach安全随机数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Slf4j
public final class PeachSecureRandom {

    private static final SecureRandom INSTANCE = createInstance();

    private PeachSecureRandom() {
        throw new IllegalStateException("Utility class");
    }

    private static SecureRandom createInstance() {
        try {
            return SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException ex) {
            log.warn("create secureRandom instance error ,noSuchAlgorithmException");
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
