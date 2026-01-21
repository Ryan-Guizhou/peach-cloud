package com.peach.captcha.limit;


import com.peach.captcha.model.CaptchaVO;
import com.peach.common.response.Response;




/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 10:31
 */
public interface FrequencyLimitHandler {

    /**
     * 验证码获取频率限制
     * @param captchaVO
     * @return
     */
    Response validateGet(CaptchaVO captchaVO);

    /**
     * 验证码验证频率限制
     * @param captchaVO
     * @return
     */
    Response validateCheck(CaptchaVO captchaVO);

    /**
     * 验证码验证频率限制
     * @param captchaVO
     * @return
     */
    Response validataVerify(CaptchaVO captchaVO);



}
