package com.peach.auth.common;

import com.peach.common.util.StringUtil;
import com.peach.common.util.encrypt.EncryptConst;
import com.peach.common.util.encrypt.EncryptFactory;
import com.peach.common.util.encrypt.EncryptService;

import java.security.GeneralSecurityException;
import java.util.regex.Pattern;

/**
 * 用户敏感字段加解密，底层统一使用 peach-common AES 能力。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public final class SensitiveFieldCipher {

    private static final EncryptService AES = EncryptFactory.getEncrypt(EncryptConst.AES);

    private static final Pattern HEX_CIPHER_PATTERN = Pattern.compile("^[0-9a-fA-F]{32,}$");

    private static final int AES_BLOCK_HEX_LENGTH = 32;

    private static final Pattern VERSIONED_AES_CIPHER_PATTERN = Pattern.compile("^v1:AES-GCM:(?:[0-9a-fA-F]{2}){28,}$");

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    private SensitiveFieldCipher() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 判断字段值是否已按 AES 密文格式存储。
     *
     * @param value 待判断值
     * @return 符合密文特征且不像常见明文格式时返回 {@code true}
     */
    public static boolean isEncrypted(String value) {
        if (StringUtil.isBlank(value)) {
            return false;
        }
        String normalized = value.trim();
        if (VERSIONED_AES_CIPHER_PATTERN.matcher(normalized).matches()) {
            return true;
        }
        if (isPhone(normalized) || isEmail(normalized)) {
            return false;
        }
        return HEX_CIPHER_PATTERN.matcher(normalized).matches()
                && normalized.length() % AES_BLOCK_HEX_LENGTH == 0;
    }

    /**
     * 加密敏感字段；已加密或空值原样返回。
     *
     * @param plainText 明文
     * @return 十六进制密文
     */
    public static String encrypt(String plainText) {
        if (StringUtil.isBlank(plainText)) {
            return plainText;
        }
        String normalized = plainText.trim();
        if (isEncrypted(normalized)) {
            return normalized;
        }
        try {
            return AES.encrypt(normalized);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt sensitive field", exception);
        }
    }

    /**
     * 解密敏感字段；历史明文或解密失败时原样返回，兼容存量数据。
     *
     * @param cipherText 密文或历史明文
     * @return 解密后的明文，或原值
     */
    public static String decrypt(String cipherText) {
        if (StringUtil.isBlank(cipherText) || !isEncrypted(cipherText)) {
            return cipherText;
        }
        try {
            return AES.decrypt(cipherText.trim());
        } catch (GeneralSecurityException exception) {
            return cipherText;
        }
    }

    /**
     * 判断是否为大陆手机号格式。
     *
     * @param value 待判断值
     * @return 匹配手机号规则时返回 {@code true}
     */
    public static boolean isPhone(String value) {
        return StringUtil.isNotBlank(value) && PHONE_PATTERN.matcher(value.trim()).matches();
    }

    /**
     * 判断是否为常见邮箱格式。
     *
     * @param value 待判断值
     * @return 匹配邮箱规则时返回 {@code true}
     */
    public static boolean isEmail(String value) {
        return StringUtil.isNotBlank(value) && EMAIL_PATTERN.matcher(value.trim()).matches();
    }
}
