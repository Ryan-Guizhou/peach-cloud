package com.peach.common.util.encrypt;

import java.security.GeneralSecurityException;

/**
 * 对称加解密服务契约。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public interface EncryptService {

    /**
     * 加密明文。
     *
     * @param plainText 明文
     * @return 十六进制密文
     * @throws GeneralSecurityException 底层 Cipher 初始化或运算失败
     */
    String encrypt(String plainText) throws GeneralSecurityException;

    /**
     * 解密十六进制密文。
     *
     * @param cipherText 十六进制密文
     * @return 明文
     * @throws GeneralSecurityException 底层 Cipher 初始化或运算失败
     */
    String decrypt(String cipherText) throws GeneralSecurityException;
}
