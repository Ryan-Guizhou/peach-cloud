package com.peach.common.util.encrypt.support;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

/**
 * BouncyCastle Provider 注册支持，供 SM4 等国密算法使用。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public final class BouncyCastleProviderSupport {

    private static final Logger log = LoggerFactory.getLogger(BouncyCastleProviderSupport.class);

    private static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;

    private BouncyCastleProviderSupport() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 返回 BouncyCastle Provider 名称，必要时完成注册。
     *
     * @return Provider 名称，通常为 {@code BC}
     */
    public static String providerName() {
        if (Security.getProvider(PROVIDER_NAME) == null) {
            synchronized (BouncyCastleProviderSupport.class) {
                if (Security.getProvider(PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                    log.debug("Registered BouncyCastle security provider");
                }
            }
        }
        return PROVIDER_NAME;
    }
}
