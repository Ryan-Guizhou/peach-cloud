package com.peach.common.util.encrypt;

import java.security.GeneralSecurityException;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:10
 */
@Slf4j
public class EncryptFactory {

    private static final Map<String, EncryptProvider> PROVIDER_MAP = new ConcurrentHashMap<>();

    private EncryptFactory() {
        throw new IllegalStateException("Utility class");
    }

    static {
        ServiceLoader<EncryptProvider> load = ServiceLoader.load(EncryptProvider.class);
        for (EncryptProvider provider : load) {
            PROVIDER_MAP.put(provider.type(), provider);
        }
    }

    public static EncryptService getEncrypt(String type) {
        EncryptProvider provider = PROVIDER_MAP.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for type: " + type);
        }
        return provider.getEncrypt();
    }

}
