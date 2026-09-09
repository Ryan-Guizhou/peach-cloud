package com.peach.common.util.encrypt;

import com.peach.common.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * 加密密钥解析器，按系统属性、环境变量、classpath 默认文件顺序加载。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public final class EncryptKeyResolver {

    private EncryptKeyResolver() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 解析并校验指定算法的密钥字节。
     *
     * @param profile 密钥配置
     * @return 密钥材料
     * @throws IllegalStateException 未配置密钥或长度不符合要求时抛出
     */
    public static byte[] resolve(EncryptKeyProfile profile) {
        String keyMaterial = readKeyMaterial(profile);
        byte[] keyBytes = decodeKeyMaterial(keyMaterial);
        validateKeyLength(profile, keyBytes);
        return keyBytes;
    }

    /**
     * 读取密钥材料
     *
     * @param profile
     * @return
     */
    private static String readKeyMaterial(EncryptKeyProfile profile) {
        String keyMaterial = System.getProperty(profile.propertyKey());
        if (StringUtil.isNotBlank(keyMaterial)) {
            return keyMaterial.trim();
        }

        keyMaterial = System.getenv(profile.envKey());
        if (StringUtil.isNotBlank(keyMaterial)) {
            return keyMaterial.trim();
        }

        try (InputStream inputStream = EncryptKeyResolver.class.getResourceAsStream(profile.defaultKeyFile())) {
            if (inputStream != null) {
                keyMaterial = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                if (StringUtil.isNotBlank(keyMaterial)) {
                    return keyMaterial.trim();
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(profile.label() + " default encryption key cannot be read", exception);
        }

        throw new IllegalStateException(profile.label() + " encryption key is not configured");
    }

    /**
     * 解密密钥材料
     * @param keyMaterial
     * @return
     */
    private static byte[] decodeKeyMaterial(String keyMaterial) {
        if (keyMaterial.startsWith(EncryptConst.BASE64_KEY_PREFIX)) {
            return Base64.getDecoder().decode(keyMaterial.substring(EncryptConst.BASE64_KEY_PREFIX.length()));
        }
        return keyMaterial.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 校验密钥的长度
     * @param profile
     * @param keyBytes
     */
    private static void validateKeyLength(EncryptKeyProfile profile, byte[] keyBytes) {
        if (profile == EncryptKeyProfile.DES) {
            if (keyBytes.length < profile.allowedKeyLengths()[0]) {
                throw new IllegalStateException(profile.label() + " encryption key must be at least 8 bytes");
            }
            return;
        }

        boolean matched = Arrays.stream(profile.allowedKeyLengths()).anyMatch(length -> length == keyBytes.length);
        if (!matched) {
            throw new IllegalStateException(
                    profile.label() + " encryption key length must be one of " + Arrays.toString(profile.allowedKeyLengths()) + " bytes"
            );
        }
    }
}
