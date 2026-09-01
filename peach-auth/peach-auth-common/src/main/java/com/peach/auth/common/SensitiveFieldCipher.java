package com.peach.auth.common;

import com.peach.common.util.StringUtil;
import com.peach.common.util.encrypt.EncryptConst;
import com.peach.common.util.encrypt.EncryptFactory;
import com.peach.common.util.encrypt.EncryptService;

import java.security.GeneralSecurityException;
import java.util.regex.Pattern;

/**
 * 用户敏感字段加解密，底层统一使用 peach-common 的 AES 能力。
 */
public final class SensitiveFieldCipher {

    private static final EncryptService AES = EncryptFactory.getEncrypt(EncryptConst.AES);

    private static final Pattern ENCRYPTED_PATTERN = Pattern.compile("^[0-9a-fA-F]+:[0-9a-fA-F]+$");

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    private SensitiveFieldCipher() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isEncrypted(String value) {
        return StringUtil.isNotBlank(value) && ENCRYPTED_PATTERN.matcher(value.trim()).matches();
    }

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

    public static boolean isPhone(String value) {
        return StringUtil.isNotBlank(value) && PHONE_PATTERN.matcher(value.trim()).matches();
    }

    public static boolean isEmail(String value) {
        return StringUtil.isNotBlank(value) && EMAIL_PATTERN.matcher(value.trim()).matches();
    }
}
