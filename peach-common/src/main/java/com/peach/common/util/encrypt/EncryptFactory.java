package com.peach.common.util.encrypt;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:10
 */
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
            throw new RuntimeException("No provider found for type: " + type);
        }
        return provider.getEncrypt();
    }

    public static void main(String[] args) throws Exception {
        EncryptService encrypt = EncryptFactory.getEncrypt(EncryptConst.DES);
        String encryptStr = encrypt.encrypt("123456");
        System.out.println(encryptStr);
        System.out.println(encrypt.decrypt(encryptStr));
    }
}
