package com.peach.rocket.security;

import com.peach.rocket.exception.MqException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AesGcmMQ载荷加密器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class AesGcmMqPayloadEncryptor implements MqPayloadEncryptor {

    private static final int IV_LENGTH = 12;

    private static final int TAG_LENGTH_BIT = 128;

    private final MqKeyProvider keyProvider;

    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmMqPayloadEncryptor(MqKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @Override
    public MqEncryptionResult encrypt(byte[] plainBytes, MqEncryptionContext context) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyProvider.getKey(context.keyId()), "AES"), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] encrypted = cipher.doFinal(plainBytes);
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv).put(encrypted);
            return new MqEncryptionResult(buffer.array(), context.algorithm(), context.keyId());
        } catch (Exception ex) {
            throw new MqException("Failed to encrypt MQ payload", ex);
        }
    }

    @Override
    public byte[] decrypt(byte[] cipherBytes, MqEncryptionContext context) {
        try {
            byte[] iv = Arrays.copyOfRange(cipherBytes, 0, IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(cipherBytes, IV_LENGTH, cipherBytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyProvider.getKey(context.keyId()), "AES"), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return cipher.doFinal(encrypted);
        } catch (Exception ex) {
            throw new MqException("Failed to decrypt MQ payload", ex);
        }
    }
}
