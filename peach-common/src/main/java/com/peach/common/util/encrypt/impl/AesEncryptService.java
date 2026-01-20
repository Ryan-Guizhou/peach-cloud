package com.peach.common.util.encrypt.impl;

import com.peach.common.util.encrypt.AbstractEncrypt;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:12
 */
public class AesEncryptService extends AbstractEncrypt {

    /**
     * 私钥
     */
    private final static String PRIVATE_KEY = "PEACH/COMMON/202503/Ryan_Guizhou";


    /**
     * 加密模式
     */
    private static final String CBC_MODEL = "AES/CBC/PKCS5Padding";

    /**
     * 偏移量 必须是8位
     */
    private static final String IV_STRING = "SHA1PRNG20250313";

    private final String algorithm;

    public AesEncryptService(String algorithm) {
        this.algorithm = algorithm;
    }


    @Override
    public String encrypt(String plainText) throws Exception {
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return byteToHex(encryptedBytes);
    }

    @Override
    public String decrypt(String cipherText) throws Exception {
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
        byte[] decodedBytes = hexToByte(cipherText);
        return new String(cipher.doFinal(decodedBytes), StandardCharsets.UTF_8);
    }

    @Override
    public Map<String, String> getRsaInfo() throws Exception {
        return Collections.emptyMap();
    }

    /**
     * 初始化 AES Cipher
     * @param mode 加密/解密模式
     * @return Cipher 对象
     */
    private Cipher initCipher(int mode) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(PRIVATE_KEY.getBytes(StandardCharsets.UTF_8), algorithm);
        IvParameterSpec iv = new IvParameterSpec(IV_STRING.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance(CBC_MODEL);
        cipher.init(mode, secretKey, iv);
        return cipher;
    }
}
