package com.peach.auth.service.support;

import com.peach.auth.common.RsaPasswordUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 将受信 RSA 密码密文解密为仅限当前校验作用域使用的明文。
 *
 * <p>捕获并统一处理解密异常，禁止外泄密文、原始异常及密钥细节。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 11:00
 */
@Component
public class PasswordCipherService {

    /**
     * 解密来自 HTTPS 请求的 RSA 密文。
     *
     * @param ciphertext 用公开登录密钥加密的密码密文
     * @return 只供当前校验使用的明文
     */
    public String decrypt(String ciphertext) {
        if (!StringUtils.hasText(ciphertext) || ciphertext.length() > 1024) {
            throw new SecurityException("Password ciphertext is invalid");
        }
        try {
            String raw = RsaPasswordUtil.decrypt(ciphertext);
            if (!StringUtils.hasText(raw)) {
                throw new SecurityException("Password ciphertext is invalid");
            }
            return raw;
        } catch (Exception exception) {
            throw new SecurityException("Password ciphertext is invalid");
        }
    }
}
