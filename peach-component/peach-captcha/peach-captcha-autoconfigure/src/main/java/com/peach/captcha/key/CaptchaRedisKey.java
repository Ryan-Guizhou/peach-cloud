package com.peach.captcha.key;

import com.peach.common.key.KeyDefinition;

/**
 * 验证码组件的 Redis Key 定义。
 * <p>
 * 该枚举只维护 peach-captcha 内部使用的 key 模板和说明信息，通过 peach-common 的
 * {@link com.peach.common.key.KeyBuilder} 统一格式化最终 key。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public enum CaptchaRedisKey implements KeyDefinition {

    /** 验证码请求频率限制 key。 */
    CAPTCHA_REQ_LIMIT("PEACH:CAPTCHA:REQ:LIMIT:{0}-{1}", "CAPTCHA", "用户请求验证码的次数", "请求次数", "Ryan"),

    /** 首次验证码校验 token key。 */
    RUNNING_CAPTCHA("PEACH:CAPTCHA:RUNNING:{0}", "CAPTCHA", "一次验证码的一次性 token", "验证码有效期秒数", "Ryan"),

    /** 二次验证码校验 token key。 */
    RUNNING_CAPTCHA_SECOND("PEACH:CAPTCHA:RUNNING:SECOND:{0}", "CAPTCHA", "二次校验验证码的一次性 token", "验证码有效期秒数", "Ryan");

    private final String pattern;

    private final String moduleCode;

    private final String keyIntroduce;

    private final String valueIntroduce;

    private final String author;

    CaptchaRedisKey(String pattern, String moduleCode, String keyIntroduce, String valueIntroduce, String author) {
        this.pattern = pattern;
        this.moduleCode = moduleCode;
        this.keyIntroduce = keyIntroduce;
        this.valueIntroduce = valueIntroduce;
        this.author = author;
    }

    @Override
    public String pattern() {
        return pattern;
    }

    @Override
    public String moduleCode() {
        return moduleCode;
    }

    @Override
    public String keyIntroduce() {
        return keyIntroduce;
    }

    @Override
    public String valueIntroduce() {
        return valueIntroduce;
    }

    @Override
    public String author() {
        return author;
    }
}
