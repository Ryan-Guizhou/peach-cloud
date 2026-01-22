package com.peach.common.util;

import com.peach.common.constant.PubCommonConst;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 17:26
 * @Description Base64工具类
 * 因为编码器的不同,在对有特殊符号 如: +- 进行base64编码时,会出现乱码,所以需要使用更加安全的编码器 urlEncoder/urlDecoder
 */
public final class Base64Util {

    private static final String DEFAULT_CHARSET = PubCommonConst.UTF_8;

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

    public static String encodeToString(byte[] src) throws UnsupportedEncodingException {
        return src.length == 0 ? "" : new String(encode(src), DEFAULT_CHARSET);
    }

    public static byte[] decodeFromString(String src) throws UnsupportedEncodingException {
        return StringUtil.isBlank(src) ? new byte[0] : decode(src.getBytes(DEFAULT_CHARSET));
    }

    public static String safeEncodeToString(byte[] src) throws UnsupportedEncodingException {
        return src.length == 0 ? "" : new String(safeEncode(src), DEFAULT_CHARSET);
    }

    public static byte[] safeDecodeFromString(String src) throws UnsupportedEncodingException {
        return StringUtil.isBlank(src) ? new byte[0] : safeDecode(src.getBytes(DEFAULT_CHARSET));
    }
}
