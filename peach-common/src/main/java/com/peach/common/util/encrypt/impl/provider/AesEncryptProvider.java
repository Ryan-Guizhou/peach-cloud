package com.peach.common.util.encrypt.impl.provider;

import com.peach.common.util.encrypt.EncryptConst;
import com.peach.common.util.encrypt.EncryptProvider;
import com.peach.common.util.encrypt.EncryptService;
import com.peach.common.util.encrypt.impl.AesEncryptService;

/**
 * AesEncrypt提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:18
 */
public class AesEncryptProvider implements EncryptProvider {

    @Override
    public String type() {
        return EncryptConst.AES;
    }

    @Override
    public EncryptService getEncrypt() {
        return new AesEncryptService(type());
    }
}
