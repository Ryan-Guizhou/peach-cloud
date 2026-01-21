package com.peach.captcha.service;

import com.peach.captcha.model.CaptchaVO;
import com.peach.common.response.Response;

import java.util.Properties;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 14:52
 */
public interface CaptchaService {

    /**
     * 配置初始化
     * @param config 配置
     */
    void init(Properties config);

    /**
     * 获取验证码
     * @param captchaVO 数据
     * @return 结果
     */
    Response get(CaptchaVO captchaVO);

    /**
     * 核对验证码(前端)
     * @param captchaVO 数据
     * @return 结果
     */
    Response check(CaptchaVO captchaVO);

    /**
     * 二次校验验证码(后端)
     * @param captchaVO 数据
     * @return 结果
     */
    Response verification(CaptchaVO captchaVO);


}
