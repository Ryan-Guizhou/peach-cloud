package com.peach.common.util.encrypt;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 抽象加密。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:10
 */
@Slf4j
public abstract class AbstractEncrypt implements EncryptService {

    /**
     * 16进制转byte
     * @param ciphertext
     * @return
     */
    protected byte[] hexToByte(String ciphertext) {
        byte[] cipherBytes = ciphertext.getBytes(StandardCharsets.UTF_8);
        if ((cipherBytes.length % 2) != 0) {
            log.warn("Ciphertext length is not even, length={}", cipherBytes.length);
            throw new IllegalArgumentException("Ciphertext length is not even");
        }
        byte[] result = new byte[cipherBytes.length / 2];
        for (int i = 0; i < cipherBytes.length; i += 2) {
            String item = new String(cipherBytes, i, 2);
            result[i / 2] = (byte) Integer.parseInt(item, 16);
        }
        return result;

    }

    /**
     * byte 转16进制
     * @param bytes
     * @return
     */
    protected String byteToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        if (bytes.length == 0){
            return stringBuilder.toString();
        }
        for (int i = 0; i < bytes.length; i++) {
            String s = Integer.toHexString(bytes[i] & 0xFF);
            if (1 == s.length()) {
                stringBuilder.append("0");
            }
            stringBuilder.append(s);
        }
        return stringBuilder.toString();

    }
}
