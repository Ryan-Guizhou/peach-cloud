package com.peach.captcha.model;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码视图对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 11:06
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "验证码视图对象")
public class CaptchaVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -44213028347206787L;

    @Schema(description = "客户端唯一标识")
    private String clientUid;

    @Schema(description = "客户端IP和User-Agent摘要")
    private String browserInfo;

    @Schema(description = "验证码类型")
    private String captchaType;

    @Schema(description = "验证码令牌")
    private String token;

    @Schema(description = "后台二次校验参数")
    private String captchaVerification;


    @Schema(description = "验证码密钥")
    private String secretKey;

    @Schema(description = "原始图片Base64")
    private String slidingOriginalImageBase64;

    @Schema(description = "滑块图片Base64")
    private String newSlidingBlockingImageBase64;

    @Schema(description = "点选文字底图Base64")
    private String picClickBase64;

    @Schema(description = "点选文字提示图Base64")
    private String picClickpromptBase64;

    @Schema(description = "点坐标加密传输内容")
    private String answer;

    @Schema(description = "旋转图片Base64")
    private String rotateImageBase64;

    public void resetClientFlag(){
        this.browserInfo = null;
        this.clientUid = null;
    }

    @Schema(description = "验证码校验结果")
    private Boolean result;

}
