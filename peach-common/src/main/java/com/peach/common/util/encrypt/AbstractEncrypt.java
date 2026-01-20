package com.peach.common.util.encrypt;

import lombok.extern.slf4j.Slf4j;

/**
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
        byte[] cipherBytes = ciphertext.getBytes();
        if ((cipherBytes.length % 2) != 0) {
            log.error("The content:[{}] length is not even",ciphertext);
            throw new IllegalArgumentException(String.format("The content:[%s] length is not even",ciphertext));
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
        StringBuffer stringBuffer = new StringBuffer();
        if (bytes.length == 0){
            return new String(stringBuffer);
        }
        for (int i = 0; i < bytes.length; i++) {
            String s = Integer.toHexString(bytes[i] & 0xFF);
            if (1 == s.length()) {
                stringBuffer.append("0");
            }
            stringBuffer = stringBuffer.append(s);
        }
        return new String(stringBuffer);

    }
}
