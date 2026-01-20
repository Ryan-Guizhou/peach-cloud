package com.peach.common.util.encrypt;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:14
 */
public interface EncryptProvider {

    /**
     * 加密类型
     * @return 加密类型
     */
    String type();

    /**
     * 获取加密服务
     * @return 加密服务
     */
    EncryptService getEncrypt();
}
