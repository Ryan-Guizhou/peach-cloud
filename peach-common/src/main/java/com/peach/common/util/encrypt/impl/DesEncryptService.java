package com.peach.common.util.encrypt.impl;

import com.peach.common.util.encrypt.AbstractEncrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:25
 */
public class DesEncryptService extends AbstractEncrypt {

    /**
     * 私钥
     */
    private static final String PRIVATE_KEY = "PEACH/COMMON/20250313/Ryan_Guizou";


    /**
     * 加密模式
     */
    private static final String CBC_MODEL = "DES/CBC/PKCS5Padding";

    /**
     * 偏移量，必须为 8 位
     */
    private static final String IV_STRING = "SHA1PRNG";

    private final String type;

    public DesEncryptService(String type) {
        this.type = type;
    }

    @Override
    public String encrypt(String plaintext) throws Exception {
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
        byte[] bytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return byteToHex(bytes);
    }

    /**
     * 解密
     * @param plaintext 密文
     * @return 明文
     * @throws Exception 解密异常
     */
    @Override
    public String decrypt(String plaintext) throws Exception {
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
        byte[] bytes = hexToByte(plaintext);
        return new String(cipher.doFinal(bytes), StandardCharsets.UTF_8);
    }

    @Override
    public Map<String, String> getRsaInfo() throws Exception {
        return Map.of();
    }

    /**
     * 初始化 Cipher
     * @param mode 模式
     * @return Cipher
     * @throws Exception 初始化异常
     */
    private Cipher initCipher(int mode) throws Exception {
        SecureRandom secureRandom = new SecureRandom();
        DESKeySpec desKeySpec = new DESKeySpec(PRIVATE_KEY.getBytes(StandardCharsets.UTF_8));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(type);
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        IvParameterSpec iv = new IvParameterSpec(IV_STRING.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance(CBC_MODEL);
        cipher.init(mode, secretKey, iv, secureRandom);
        return cipher;
    }

}
