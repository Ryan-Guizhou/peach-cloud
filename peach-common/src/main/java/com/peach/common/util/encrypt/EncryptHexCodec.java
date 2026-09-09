package com.peach.common.util.encrypt;

import java.nio.charset.StandardCharsets;

/**
 * 加解密模块使用的十六进制编解码工具。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public final class EncryptHexCodec {

    private EncryptHexCodec() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 将十六进制字符串解码为字节数组。
     *
     * @param hexText 十六进制文本
     * @return 解码结果
     * @throws IllegalArgumentException 长度不为偶数或包含非法字符时抛出
     */
    public static byte[] hexToBytes(String hexText) {
        byte[] hexChars = hexText.getBytes(StandardCharsets.UTF_8);
        if ((hexChars.length % 2) != 0) {
            throw new IllegalArgumentException("Hex cipher text length is not even");
        }
        byte[] result = new byte[hexChars.length / 2];
        for (int index = 0; index < hexChars.length; index += 2) {
            String item = new String(hexChars, index, 2, StandardCharsets.UTF_8);
            result[index / 2] = (byte) Integer.parseInt(item, 16);
        }
        return result;
    }

    /**
     * 将字节数组编码为十六进制字符串。
     *
     * @param bytes 原始字节
     * @return 小写十六进制文本
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            String hex = Integer.toHexString(value & 0xFF);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }
}
