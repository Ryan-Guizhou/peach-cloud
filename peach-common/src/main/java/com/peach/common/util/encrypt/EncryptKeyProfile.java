package com.peach.common.util.encrypt;

/**
 * 各算法密钥加载配置，供 {@link EncryptKeyResolver} 使用。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public enum EncryptKeyProfile {

    AES(
            EncryptConst.AES,
            EncryptConst.AES_KEY_PROPERTY,
            EncryptConst.AES_KEY_ENV,
            EncryptConst.AES_DEFAULT_KEY_FILE,
            16, 24, 32
    ),

    DES(
            EncryptConst.DES,
            EncryptConst.DES_KEY_PROPERTY,
            EncryptConst.DES_KEY_ENV,
            EncryptConst.DES_DEFAULT_KEY_FILE,
            8
    ),

    SM4(
            EncryptConst.SM4,
            EncryptConst.SM4_KEY_PROPERTY,
            EncryptConst.SM4_KEY_ENV,
            EncryptConst.SM4_DEFAULT_KEY_FILE,
            16
    );

    private final String label;

    private final String propertyKey;

    private final String envKey;

    private final String defaultKeyFile;

    private final int[] allowedKeyLengths;

    EncryptKeyProfile(
            String label,
            String propertyKey,
            String envKey,
            String defaultKeyFile,
            int... allowedKeyLengths
    ) {
        this.label = label;
        this.propertyKey = propertyKey;
        this.envKey = envKey;
        this.defaultKeyFile = defaultKeyFile;
        this.allowedKeyLengths = allowedKeyLengths;
    }

    public String label() {
        return label;
    }

    public String propertyKey() {
        return propertyKey;
    }

    public String envKey() {
        return envKey;
    }

    public String defaultKeyFile() {
        return defaultKeyFile;
    }

    public int[] allowedKeyLengths() {
        return allowedKeyLengths;
    }
}
