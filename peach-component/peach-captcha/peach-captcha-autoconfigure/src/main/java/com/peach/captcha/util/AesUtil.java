package com.peach.captcha.util;

import org.apache.commons.lang3.StringUtils;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import com.peach.common.util.PeachSecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Aes工具类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/21 18:06
 */
public final class AesUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int GCM_IV_LENGTH = 12;

    private static final int GCM_TAG_LENGTH = 128;

    private AesUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取随机key
     * @return
     */
    public static String getKey() {
        return RandomUtils.getRandomString(16);
    }


    /**
     * 将byte[]转为各种进制的字符串
     * @param bytes byte[]
     * @param radix 可以转换进制的范围，从Character.MIN_RADIX到Character.MAX_RADIX，超出范围后变为10进制
     * @return 转换后的字符串
     */
    public static String binary(byte[] bytes, int radix) {
        // 这里的1代表正数
        return new BigInteger(1, bytes).toString(radix);
    }

    /**
     * base 64 encode
     * @param bytes 待编码的byte[]
     * @return 编码后的base 64 code
     */
    public static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * base 64 decode
     * @param base64Code 待解码的base 64 code
     * @return 解码后的byte[]
     */
    public static byte[] base64Decode(String base64Code) {
        Base64.Decoder decoder = Base64.getDecoder();
        return StringUtils.isEmpty(base64Code) ? null : decoder.decode(base64Code);
    }


    /**
     * AES加密
     * @param content 待加密的内容
     * @param encryptKey 加密密钥
     * @return 加密后的byte[]
     */
    public static byte[] aesEncryptToBytes(String content, String encryptKey) throws GeneralSecurityException {
        byte[] iv = new byte[GCM_IV_LENGTH];
        PeachSecureRandom.get().nextBytes(iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(encryptKey.getBytes(StandardCharsets.UTF_8), "AES"),
                parameterSpec);
        byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return result;
    }


    /**
     * AES加密为base 64 code
     * @param content 待加密的内容
     * @param encryptKey 加密密钥
     * @return 加密后的base 64 code
     */
    public static String aesEncrypt(String content, String encryptKey) throws GeneralSecurityException {
        if (StringUtils.isBlank(encryptKey)) {
            return content;
        }
        return base64Encode(aesEncryptToBytes(content, encryptKey));
    }

    /**
     * AES解密
     * @param encryptBytes 待解密的byte[]
     * @param decryptKey 解密密钥
     * @return 解密后的String
     */
    public static String aesDecryptByBytes(byte[] encryptBytes, String decryptKey) throws GeneralSecurityException {
        byte[] iv = Arrays.copyOfRange(encryptBytes, 0, GCM_IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(encryptBytes, GCM_IV_LENGTH, encryptBytes.length);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(decryptKey.getBytes(StandardCharsets.UTF_8), "AES"),
                parameterSpec);
        byte[] decryptBytes = cipher.doFinal(ciphertext);
        return new String(decryptBytes, StandardCharsets.UTF_8);
    }


    /**
     * 将base 64 code AES解密
     * @param encryptStr 待解密的base 64 code
     * @param decryptKey 解密密钥
     * @return 解密后的string
     */
    public static String aesDecrypt(String encryptStr, String decryptKey) throws GeneralSecurityException {
        if (StringUtils.isBlank(decryptKey)) {
            return encryptStr;
        }
        if (StringUtils.isEmpty(encryptStr)) {
            return null;
        }
        byte[] encryptedBytes = base64Decode(encryptStr);
        if (encryptedBytes == null || encryptedBytes.length == 0) {
            return null;
        }
        return aesDecryptByBytes(encryptedBytes, decryptKey);
    }

}
