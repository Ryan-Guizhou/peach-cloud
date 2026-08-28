package com.peach.common.util.encrypt.impl;

import com.peach.common.util.StringUtil;
import com.peach.common.util.encrypt.AbstractEncrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import com.peach.common.util.PeachSecureRandom;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;

/**
 * AesEncrypt 服务类。
 * <p>密钥必须在运行时通过系统属性 {@code peach.common.encrypt.aes.key}
 * 或环境变量 {@code PEACH_COMMON_ENCRYPT_AES_KEY} 提供。
 * 当密钥为 Base64 编码时，请使用 {@code base64:<值>} 格式。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public class AesEncryptService extends AbstractEncrypt {

    private static final String KEY_PROPERTY = "peach.common.encrypt.aes.key";

    private static final String KEY_ENV = "PEACH_COMMON_ENCRYPT_AES_KEY";

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int GCM_IV_LENGTH_BYTES = 12;

    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final String PAYLOAD_SEPARATOR = ":";

    private final String algorithm;

    public AesEncryptService(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("AES key algorithm must not be blank");
        }
        this.algorithm = algorithm;
    }

    @Override
    public String encrypt(String plainText) throws GeneralSecurityException {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        PeachSecureRandom.get().nextBytes(iv);
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, iv);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return byteToHex(iv) + PAYLOAD_SEPARATOR + byteToHex(encryptedBytes);
    }

    @Override
    public String decrypt(String cipherText) throws GeneralSecurityException {
        String[] payload = cipherText.split(PAYLOAD_SEPARATOR, 2);
        if (payload.length != 2) {
            throw new IllegalArgumentException("AES cipher text is missing initialization vector");
        }
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE, hexToByte(payload[0]));
        byte[] decodedBytes = hexToByte(payload[1]);
        return new String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8);
    }

    @Override
    public Map<String, String> getRsaInfo() {
        return Map.of();
    }

    private Cipher initCipher(int mode, byte[] iv) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(resolveKey(), algorithm);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(mode, secretKey, gcmParameterSpec);
        return cipher;
    }

    private byte[] resolveKey() {
        String configuredKey = System.getProperty(KEY_PROPERTY);
        if (StringUtil.isBlank(configuredKey)) {
            configuredKey = System.getenv(KEY_ENV);
        }
        if (StringUtil.isBlank(configuredKey)) {
            throw new IllegalStateException("AES encryption key is not configured");
        }
        byte[] keyBytes = configuredKey.startsWith("base64:")
                ? Base64.getDecoder().decode(configuredKey.substring("base64:".length()))
                : configuredKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("AES encryption key must be 16, 24 or 32 bytes");
        }
        return keyBytes;
    }
}
