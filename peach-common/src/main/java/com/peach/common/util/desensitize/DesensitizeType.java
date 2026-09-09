package com.peach.common.util.desensitize;

/**
 * 常用脱敏类型。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:20
 */
public enum DesensitizeType {

    /**
     * 手机号，保留前 3 后 4。
     */
    MOBILE,

    /**
     * 邮箱，保留本地部分前 2 位及完整域名。
     */
    EMAIL,

    /**
     * 身份证号，保留前 6 后 4。
     */
    ID_CARD,

    /**
     * 银行卡号，保留前 4 后 4。
     */
    BANK_CARD,

    /**
     * 姓名，保留首尾字符。
     */
    NAME,

    /**
     * 密钥/密码，保留末尾最多 4 位。
     */
    SECRET,

    /**
     * 地址，保留前 6 个字符。
     */
    ADDRESS,

    /**
     * IPv4，保留前两段。
     */
    IP,

    /**
     * 通用中间掩码，保留前 3 后 3。
     */
    GENERAL
}
