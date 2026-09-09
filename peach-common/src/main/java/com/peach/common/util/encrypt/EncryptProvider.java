package com.peach.common.util.encrypt;

/**
 * 加解密实现 SPI 提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:14
 */
public interface EncryptProvider {

    /**
     * 支持的算法类型标识。
     *
     * @return 算法类型，例如 {@link EncryptConst#AES}
     */
    String type();

    /**
     * 创建对应算法的加解密服务实例。
     *
     * @return 加解密服务
     */
    EncryptService getEncrypt();
}
