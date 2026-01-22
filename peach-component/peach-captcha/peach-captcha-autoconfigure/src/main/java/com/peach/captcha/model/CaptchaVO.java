package com.peach.captcha.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 11:06
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 4709594363282708784L;

    private String clientUid;

    /***
     * 客户端ip+userAgent
     */
    private String browserInfo;

    private String captchaType;

    private String token;

    /**
     * 后台二次校验参数
     */
    private String captchaVerification;


    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 原生图片base64
     */
    private String slidingOriginalImageBase64;

    /**
     * 滑块图片base64
     */
    private String newSlidingBlockingImageBase64;

    /**
     * 点选文字底图
     */
    private String picClickBase64;

    /**
     * 点选文字提示图
     */
    private String picClickpromptBase64;

    /**
     * 点坐标(base64加密传输)
     */
    private String answer;

    /**
     * 旋转图片base64
     */
    private String rotateImageBase64;

    public void resetClientFlag(){
        this.browserInfo = null;
        this.clientUid = null;
    }

    /**
     * 校验结果
     */
    private Boolean result;

}
