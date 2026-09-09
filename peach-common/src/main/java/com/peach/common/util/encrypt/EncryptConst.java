package com.peach.common.util.encrypt;

/**
 * 加解密相关的全局常量定义类。
 *
 * <p>包含对称加密算法（AES、DES、SM4）的名称定义，
 * 以及加解密密钥在系统属性和环境变量中的配置键名。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:11
 */
public final class EncryptConst {

    private EncryptConst() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * AES 对称加密算法标识名称。
     */
    public static final String AES = "AES";

    /**
     * DES 对称加密算法标识名称。
     */
    public static final String DES = "DES";

    /**
     * SM4 对称加密算法标识名称。
     */
    public static final String SM4 = "SM4";

    /**
     * AES 密钥的 JVM 系统属性配置键（可以通过 -Dpeach.common.encrypt.aes.key 配置）。
     */
    public static final String AES_KEY_PROPERTY = "peach.common.encrypt.aes.key";

    /**
     * AES 密钥的操作系统环境变量配置键。
     */
    public static final String AES_KEY_ENV = "PEACH_COMMON_ENCRYPT_AES_KEY";

    /**
     * DES 密钥的 JVM 系统属性配置键（可以通过 -Dpeach.common.encrypt.des.key 配置）。
     */
    public static final String DES_KEY_PROPERTY = "peach.common.encrypt.des.key";

    /**
     * DES 密钥的操作系统环境变量配置键。
     */
    public static final String DES_KEY_ENV = "PEACH_COMMON_ENCRYPT_DES_KEY";

    /**
     * SM4 密钥的 JVM 系统属性配置键（可以通过 -Dpeach.common.encrypt.sm4.key 配置）。
     */
    public static final String SM4_KEY_PROPERTY = "peach.common.encrypt.sm4.key";

    /**
     * SM4 密钥的操作系统环境变量配置键。
     */
    public static final String SM4_KEY_ENV = "PEACH_COMMON_ENCRYPT_SM4_KEY";

    /**
     * classpath 默认密钥目录，仅作为未配置密钥时的兜底策略。
     */
    public static final String DEFAULT_KEY_ROOT = "/default/";

    /**
     * AES classpath 默认密钥文件。
     */
    public static final String AES_DEFAULT_KEY_FILE = DEFAULT_KEY_ROOT + "aes.key";

    /**
     * DES classpath 默认密钥文件。
     */
    public static final String DES_DEFAULT_KEY_FILE = DEFAULT_KEY_ROOT + "des.key";

    /**
     * SM4 classpath 默认密钥文件。
     */
    public static final String SM4_DEFAULT_KEY_FILE = DEFAULT_KEY_ROOT + "sm4.key";

    /**
     * Base64 格式密钥的前缀标识（如配置为 "base64:xxx" 时表示密钥需先进行 Base64 解码）。
     */
    public static final String BASE64_KEY_PREFIX = "base64:";
}
