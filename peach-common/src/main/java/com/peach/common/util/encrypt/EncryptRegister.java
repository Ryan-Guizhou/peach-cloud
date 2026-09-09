package com.peach.common.util.encrypt;

import com.peach.common.loader.CustomServiceLoader;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对称加解密 Provider 注册与管理中心。
 *
 * <p>通过 {@link CustomServiceLoader} 扫描并装配所有 {@link EncryptProvider} 实现。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 15:32
 */
@Slf4j
public class EncryptRegister {

    private static final Map<String, EncryptProvider> PROVIDER_MAP = new ConcurrentHashMap<>();

    static {
        CustomServiceLoader.load(EncryptProvider.class).forEach(provider -> {
            if (provider != null && StringUtil.isNotBlank(provider.type()) && !provider.type().trim().isEmpty()) {
                PROVIDER_MAP.put(normalizeType(provider.type()), provider);
                log.debug("Registered encrypt provider, type={}", provider.type());
            }
        });
    }

    private EncryptRegister() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 根据加密算法类型获取对应的加密 Provider。
     *
     * @param type 算法类型（如 AES、SM4、DES）
     * @return 对应的加密 Provider
     * @throws IllegalArgumentException 当未找到对应算法 Provider 时抛出
     */
    public static EncryptProvider getProvider(String type) {
        if (StringUtil.isBlank(type)) {
            throw new IllegalArgumentException("Encrypt type must not be blank");
        }
        EncryptProvider encryptProvider = PROVIDER_MAP.get(normalizeType(type));
        if (encryptProvider == null) {
            log.warn("No encrypt provider found for type={}", type);
            throw new IllegalArgumentException("No encrypt provider found for type: " + type);
        }
        return encryptProvider;
    }

    private static String normalizeType(String type) {
        return type.trim().toUpperCase();
    }
}
