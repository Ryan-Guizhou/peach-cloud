package com.peach.common.util.encrypt.impl;

import com.peach.common.util.StringUtil;
import com.peach.common.util.encrypt.AbstractEncrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import com.peach.common.util.PeachSecureRandom;
import java.util.Base64;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * Legacy encryption service.
 *
 * <p>This class keeps the historical constructor and EncryptService contract,
 * but no longer uses DES or a hard-coded key. The key must be provided at
 * runtime with system property {@code peach.common.encrypt.legacy.key} or
 * environment variable {@code PEACH_COMMON_ENCRYPT_LEGACY_KEY}. Use
 * {@code base64:<value>} when the key is Base64 encoded.</p>
 */
public class DesEncryptService extends AbstractEncrypt {

    private static final String KEY_PROPERTY = "peach.common.encrypt.legacy.key";

    private static final String KEY_ENV = "PEACH_COMMON_ENCRYPT_LEGACY_KEY";

    private static final String KEY_ALGORITHM = "AES";

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int GCM_IV_LENGTH_BYTES = 12;

    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final String PAYLOAD_SEPARATOR = ":";

    public DesEncryptService(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Encryption type must not be blank");
        }
    }

    @Override
    public String encrypt(String plaintext) throws GeneralSecurityException {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        PeachSecureRandom.get().nextBytes(iv);
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, iv);
        byte[] bytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return byteToHex(iv) + PAYLOAD_SEPARATOR + byteToHex(bytes);
    }

    @Override
    public String decrypt(String plaintext) throws GeneralSecurityException {
        String[] payload = plaintext.split(PAYLOAD_SEPARATOR, 2);
        if (payload.length != 2) {
            throw new IllegalArgumentException("Cipher text is missing initialization vector");
        }
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE, hexToByte(payload[0]));
        byte[] bytes = hexToByte(payload[1]);
        return new String(cipher.doFinal(bytes), StandardCharsets.UTF_8);
    }

    @Override
    public Map<String, String> getRsaInfo() {
        return Map.of();
    }

    private Cipher initCipher(int mode, byte[] iv) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(resolveKey(), KEY_ALGORITHM);
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
            throw new IllegalStateException("Legacy encryption key is not configured");
        }
        byte[] keyBytes = configuredKey.startsWith("base64:")
                ? Base64.getDecoder().decode(configuredKey.substring("base64:".length()))
                : configuredKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("Legacy encryption key must be 16, 24 or 32 bytes");
        }
        return keyBytes;
    }
}
