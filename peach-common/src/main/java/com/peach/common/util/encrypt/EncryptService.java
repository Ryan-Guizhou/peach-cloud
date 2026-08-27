package com.peach.common.util.encrypt;

import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public interface EncryptService {

    /**
     * AES 加密
     * @param plainText 明文
     * @return 加密后的 Base64 字符串
     */
    String encrypt(String plainText) throws GeneralSecurityException;


    /**
     * AES 解密
     * @param cipherText Base64 加密内容
     * @return 解密后的字符串
     */
    String decrypt(String cipherText) throws GeneralSecurityException;

    /**
     * 获取 RSA 公钥和私钥
     * @return RSA 公钥和私钥
     */
    Map<String, String> getRsaInfo();
}
