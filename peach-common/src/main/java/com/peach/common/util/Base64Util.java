package com.peach.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64工具类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 17:26
 * @Description Base64工具类
 */
public final class Base64Util {

    private Base64Util() {
        throw new IllegalStateException("Utility class");
    }

    public static byte[] encode(byte[] src) {
        return src.length == 0 ? src : Base64.getEncoder().encode(src);
    }

    public static byte[] decode(byte[] src) {
        return src.length == 0 ? src : Base64.getDecoder().decode(src);
    }

    public static byte[] safeEncode(byte[] src) {
        return src.length == 0 ? src : Base64.getUrlEncoder().encode(src);
    }

    public static byte[] safeDecode(byte[] src) {
        return src.length == 0 ? src : Base64.getUrlDecoder().decode(src);
    }

    public static String encodeToString(byte[] src) {
        return src.length == 0 ? "" : new String(encode(src), StandardCharsets.UTF_8);
    }

    /**
     * 将标准 Base64 字符串解码为字节数组。
     *
     * @param src Base64 字符串，null 或空白字符串返回空数组
     * @return 解码后的字节数组
     */
    public static byte[] decodeFromString(String src) {
        return src == null || src.isBlank() ? new byte[0] : decode(src.getBytes(StandardCharsets.UTF_8));
    }

    public static String safeEncodeToString(byte[] src) {
        return src.length == 0 ? "" : new String(safeEncode(src), StandardCharsets.UTF_8);
    }

    /**
     * 将 URL 安全 Base64 字符串解码为字节数组。
     *
     * @param src URL 安全 Base64 字符串，null 或空白字符串返回空数组
     * @return 解码后的字节数组
     */
    public static byte[] safeDecodeFromString(String src) {
        return src == null || src.isBlank() ? new byte[0] : safeDecode(src.getBytes(StandardCharsets.UTF_8));
    }
}
