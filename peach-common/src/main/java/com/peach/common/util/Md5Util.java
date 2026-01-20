package com.peach.common.util;

import java.security.MessageDigest;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 15:26
 */
public final class Md5Util {

    private Md5Util() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取指定字符串的md5值
     * @param dataStr 明文
     * @return String
     */
    public static String md5(String dataStr) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(dataStr.getBytes("UTF8"));
            byte[] s = m.digest();
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < s.length; i++) {
                result.append(Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6));
            }
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取指定字符串的md5值, md5(str+salt)
     * @param dataStr 明文
     * @return String
     */
    public static String md5WithSalt(String dataStr,String salt) {
        return md5(dataStr + salt);
    }
}
