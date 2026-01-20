package com.peach.common.util.encrypt.impl.provider;

import com.peach.common.util.encrypt.EncryptConst;
import com.peach.common.util.encrypt.EncryptProvider;
import com.peach.common.util.encrypt.EncryptService;
import com.peach.common.util.encrypt.impl.DesEncryptService;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:24
 */
public class DesEncryptProvider implements EncryptProvider {

    @Override
    public String type() {
        return EncryptConst.DES;
    }

    @Override
    public EncryptService getEncrypt() {
        return new DesEncryptService(type());
    }
}
