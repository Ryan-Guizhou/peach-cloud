package com.peach.captcha.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 13:35
 */
public interface CaptchaConst {

    String CAPTCHA_SUFFIX = "peach.captcah";

    /**
     * 获取验证码接口限流属性
     */
    String REQ_GET_MINUTE_LIMIT = "captcha.req.get.minute.limit";
     /**
      * 默认一分钟120次
      */
    String DEFAULT_REQ_GET_MINUTE_LIMIT = "120";


    /**
     * 验证失败后，get接口锁定失败次数,默认失败5次进行锁定
     */
    String REQ_GET_LOCK_FAIL = "captcha.req.get.lock.fail";

    /**
     * 默认每分钟失败5次便进行锁定
     */
    String DEFAULT_REQ_GET_LOCK_LIMIT = "5";

    /**
     * 验证失败后，get接口锁定时间
     */
    String REQ_GET_LOCK_SECONDS = "captcha.req.get.lock.seconds";

    /**
     * 默认锁定5分钟
     */
    String DEFAULT_REQ_GET_LOCK_SECONDS = "300";

    /**
     * verify 接口 一分钟请求次数限制
     */
    String REQ_VERIFY_MINUTE_LIMIT = "captcha.req.verify.minute.limit";

    /**
     * 默认一分钟600次
     */
    String DEFAULT_REQ_VERIFY_MINUTE_LIMIT = "600";

    /**
     * check接口 一分钟请求次数限制
     */
    String REQ_CHECK_MINUTE_LIMIT = "captcha.req.check.minute.limit";

    /**
     * 默认一分钟600次
     */
     String DEFAULT_REQ_CHECK_MINUTE_LIMIT = "600";

     /**
      * 验证码类型
      */
     String CAPTCHA_TYPE = "captcha.type";

     /**
      * 默认类型 default
      */
     String DEFAULT_CAPTCHA_TYPE = "default";
}
