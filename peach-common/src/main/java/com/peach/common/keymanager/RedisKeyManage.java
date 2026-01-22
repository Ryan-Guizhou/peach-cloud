package com.peach.common.keymanager;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 11:13
 */
public enum RedisKeyManage {

    CAPTCHA_REQ_LIMIT("PEACH:CAPTCHA:REQ:LIMIT:{0}-{1}", "用户请求验证码的次数", "请求次数", "CAPTCHA", "Ryan"),
    RUNNING_CAPTCHA("PEACH:CAPTCHA:RUNNING:{0}", "一次验证码的一次性token", "验证码有效期(秒数,默认120s)", "CAPTCHA", "Ryan"),
    RUNNING_CAPTCHA_SECOND("PEACH:CAPTCHA:RUNNING:SECOND:{0}", "二次校验验证码的一次性token", "验证码有效期(秒数,默认120s)", "CAPTCHA", "Ryan"),
    ;

    /**
     * key值
     * */
    private final String key;

    /**
     * key的说明
     * */
    private final String keyIntroduce;

    /**
     * value的说明
     * */
    private final String valueIntroduce;

    /**
     * 模块代码
     * */
    private final String moduleCode;

    /**
     * 作者
     */
    private final String author;


    RedisKeyManage(String key, String keyIntroduce, String valueIntroduce, String moduleCode, String author) {
        this.key = key;
        this.keyIntroduce = keyIntroduce;
        this.valueIntroduce = valueIntroduce;
        this.moduleCode = moduleCode;
        this.author = author;
    }

    public String getKey() {
        return key;
    }

    public String getKeyIntroduce() {
        return keyIntroduce;
    }

    public String getValueIntroduce() {
        return valueIntroduce;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public String getAuthor() {
        return author;
    }

}
