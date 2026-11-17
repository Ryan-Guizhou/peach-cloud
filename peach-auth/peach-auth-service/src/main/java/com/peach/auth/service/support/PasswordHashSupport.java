package com.peach.auth.service.support;

import com.peach.common.util.StringUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 账号密码的安全散列和验证实现。
 *
 * <p>保留前端 RSA 传输保护，但数据库只保存 BCrypt 散列，不再保存 Base64 密码。
 * 缺失账号或未设置密码的登录尝试均计算固定占位散列以减少认证耗时差异。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 15:35
 */
@Indexed
@Component
public class PasswordHashSupport {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private static final String DUMMY_HASH =
            "$2y$12$flEMqLM4gaYj2oNuTk2xoeFfuC0GrwIPzKpsaDgOrVKXIZ0BJasm2";

    /**
     * 将明文密码散列后持久化，不可通过 Base64 等可逆编码替代。
     *
     * @param rawPassword 已在服务端解密的密码
     * @return BCrypt 散列字符串
     */
    public String encode(String rawPassword) {
        if (StringUtil.isBlank(rawPassword)) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return ENCODER.encode(rawPassword);
    }

    /**
     * 验证用户输入与数据库散列。
     *
     * @param rawPassword 明文密码
     * @param storedHash 数据库存储的 BCrypt 散列
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null) {
            return false;
        }
        if (StringUtil.isBlank(storedHash)) {
            ENCODER.matches(rawPassword, DUMMY_HASH);
            return false;
        }
        return ENCODER.matches(rawPassword, storedHash);
    }
}
