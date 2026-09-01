package com.peach.auth.service.support;

import com.peach.auth.common.SensitiveFieldCipher;
import com.peach.auth.entity.UserDO;
import com.peach.auth.vo.UserVO;

/**
 * 用户敏感字段加解密辅助。
 */
public final class UserSensitiveFieldSupport {

    private UserSensitiveFieldSupport() {
        throw new IllegalStateException("Utility class");
    }

    public static void encryptUserFields(UserDO user) {
        if (user == null) {
            return;
        }
        user.setMobilePhone(SensitiveFieldCipher.encrypt(user.getMobilePhone()));
        user.setEmail(SensitiveFieldCipher.encrypt(user.getEmail()));
        user.setIdentityCode(SensitiveFieldCipher.encrypt(user.getIdentityCode()));
    }

    public static void decryptUserFields(UserVO user) {
        if (user == null) {
            return;
        }
        user.setMobilePhone(SensitiveFieldCipher.decrypt(user.getMobilePhone()));
        user.setEmail(SensitiveFieldCipher.decrypt(user.getEmail()));
        user.setIdentityCode(SensitiveFieldCipher.decrypt(user.getIdentityCode()));
    }
}
