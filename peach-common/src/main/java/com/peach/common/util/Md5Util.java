package com.peach.common.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Md5工具类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Slf4j
public final class Md5Util {

    private Md5Util() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取指定字符串的 SHA-256 摘要，用于需要抗碰撞的路径。
     *
     * @param dataStr 明文
     * @return SHA-256 十六进制字符串
     */
    public static String sha256Hex(String dataStr) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] digestBytes = digest.digest(dataStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digestBytes.length * 2);
            for (byte digestByte : digestBytes) {
                String hex = Integer.toHexString(digestByte & 0xFF);
                if (hex.length() == 1) {
                    result.append('0');
                }
                result.append(hex);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm is not available", e);
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
