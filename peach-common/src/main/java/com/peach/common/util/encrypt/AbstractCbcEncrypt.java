package com.peach.common.util.encrypt;

import com.peach.common.util.PeachSecureRandom;
import com.peach.common.util.StringUtil;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * 版本化对称加解密模板。
 * <p>
 * 基于 CBC 模式封装，支持 IV 随机生成与版本化 Payload 协议解析，并兼容历史旧格式密文的解密。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public abstract class AbstractCbcEncrypt implements EncryptService {

    private final EncryptKeyProfile keyProfile;

    private final String keyAlgorithm;

    private static final String PAYLOAD_SEPARATOR = ":";

    private static final String PAYLOAD_VERSION = "v1";

    protected AbstractCbcEncrypt(EncryptKeyProfile keyProfile, String keyAlgorithm) {
        if (keyProfile == null) {
            throw new IllegalArgumentException("Key profile must not be null");
        }
        if (StringUtil.isBlank(keyAlgorithm)) {
            throw new IllegalArgumentException("Key algorithm must not be blank");
        }
        this.keyProfile = keyProfile;
        this.keyAlgorithm = keyAlgorithm;
    }

    @Override
    public String encrypt(String plainText) throws GeneralSecurityException {
        if (plainText == null) {
            throw new IllegalArgumentException("Plain text must not be null");
        }
        byte[] iv = new byte[ivLengthBytes()];
        PeachSecureRandom.get().nextBytes(iv);
        Cipher cipher = createCipher(Cipher.ENCRYPT_MODE, iv);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + encryptedBytes.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encryptedBytes, 0, payload, iv.length, encryptedBytes.length);
        return PAYLOAD_VERSION + PAYLOAD_SEPARATOR + payloadAlgorithm() + PAYLOAD_SEPARATOR + EncryptHexCodec.bytesToHex(payload);
    }

    @Override
    public String decrypt(String cipherText) throws GeneralSecurityException {
        if (StringUtil.isBlank(cipherText)) {
            throw new IllegalArgumentException("Cipher text must not be blank");
        }
        byte[] decryptedBytes = isVersionedPayload(cipherText)
                ? decryptVersionedPayload(cipherText.trim())
                : decryptLegacyPayload(cipherText.trim());
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 获取加密 Payload 中标识的算法名称。
     *
     * @return 算法标识字符串（如 "AES/CBC/PKCS5Padding"）
     */
    protected abstract String payloadAlgorithm();

    /**
     * 获取初始化向量（IV）的字节长度。
     *
     * @return IV 字节长度
     */
    protected abstract int ivLengthBytes();

    /**
     * 创建已初始化的 Cipher。
     *
     * @param mode {@link Cipher#ENCRYPT_MODE} 或 {@link Cipher#DECRYPT_MODE}
     * @param iv   本次密文使用的初始化向量
     * @return Cipher 实例
     * @throws GeneralSecurityException 算法或密钥无效时抛出
     */
    protected abstract Cipher createCipher(int mode, byte[] iv) throws GeneralSecurityException;

    /**
     * 解析并获取当前配置对应的密钥字节数组。
     *
     * @return 密钥字节数组
     */
    protected byte[] resolveKeyBytes() {
        return EncryptKeyResolver.resolve(keyProfile);
    }

    /**
     * 获取对称加密算法名称。
     *
     * @return 算法名称（如 "AES", "DESede"）
     */
    protected String keyAlgorithm() {
        return keyAlgorithm;
    }

    /**
     * 创建历史 CBC 密文解密 Cipher。
     *
     * @param mode {@link Cipher#DECRYPT_MODE}
     * @return Cipher 实例
     * @throws GeneralSecurityException 算法或密钥无效时抛出
     */
    protected abstract Cipher createLegacyCipher(int mode) throws GeneralSecurityException;

    /**
     * 判断密文是否为带版本的 Payload 格式。
     *
     * @param cipherText 待检测的密文字符串
     * @return {@code true} 如果是以预设版本号开头的格式，否则 {@code false}
     */
    private boolean isVersionedPayload(String cipherText) {
        return cipherText.startsWith(PAYLOAD_VERSION + PAYLOAD_SEPARATOR);
    }

    /**
     * 解密版本化 Payload 格式的密文。
     *
     * @param cipherText 版本化密文字符串
     * @return 解密后的明文字节数组
     * @throws GeneralSecurityException 安全加解密相关异常
     * @throws IllegalArgumentException  密文结构非法、版本不匹配或载荷长度不足时抛出
     */
    private byte[] decryptVersionedPayload(String cipherText) throws GeneralSecurityException {
        String[] parts = cipherText.split(PAYLOAD_SEPARATOR, 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Cipher text payload is malformed");
        }
        if (!PAYLOAD_VERSION.equals(parts[0]) || !payloadAlgorithm().equals(parts[1])) {
            throw new IllegalArgumentException("Cipher text algorithm is not supported");
        }
        byte[] payload = EncryptHexCodec.hexToBytes(parts[2]);
        int ivLength = ivLengthBytes();
        if (payload.length <= ivLength) {
            throw new IllegalArgumentException("Cipher text payload is too short");
        }
        byte[] iv = Arrays.copyOfRange(payload, 0, ivLength);
        byte[] encryptedBytes = Arrays.copyOfRange(payload, ivLength, payload.length);
        Cipher cipher = createCipher(Cipher.DECRYPT_MODE, iv);
        return cipher.doFinal(encryptedBytes);
    }

    /**
     * 解密历史兼容格式的密文。
     *
     * @param cipherText 旧版十六进制密文字符串
     * @return 解密后的明文字节数组
     * @throws GeneralSecurityException 安全加解密相关异常
     */
    private byte[] decryptLegacyPayload(String cipherText) throws GeneralSecurityException {
        Cipher cipher = createLegacyCipher(Cipher.DECRYPT_MODE);
        return cipher.doFinal(EncryptHexCodec.hexToBytes(cipherText));
    }
}
