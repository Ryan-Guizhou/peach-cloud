package com.peach.common.util.encrypt.impl.provider;

import com.peach.common.util.encrypt.EncryptConst;
import com.peach.common.util.encrypt.EncryptProvider;
import com.peach.common.util.encrypt.EncryptService;
import com.peach.common.util.encrypt.impl.Sm4EncryptService;

/**
 * Sm4Encrypt 提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:24
 */
public class Sm4EncryptProvider implements EncryptProvider {

    @Override
    public String type() {
        return EncryptConst.SM4;
    }

    @Override
    public EncryptService getEncrypt() {
        return new Sm4EncryptService(type());
    }
}
