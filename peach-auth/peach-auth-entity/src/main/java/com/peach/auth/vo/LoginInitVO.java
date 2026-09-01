package com.peach.auth.vo;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 登录页初始化信息。
 * /
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
  */
@Data
@Schema(description = "登录页初始化信息")
public class LoginInitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -8349078546308158013L;

    @Schema(description = "系统名称")
    private String systemName;

    @Schema(description = "系统说明")
    private String systemDescription;

    @Schema(description = "初始应用ID")
    private String appId;

    @Schema(description = "当前年度")
    private Integer fiscal;

    @Schema(description = "RSA公钥，Base64编码的X.509格式")
    private String publicKey;

    @Schema(description = "密码加密算法")
    private String encryptionAlgorithm;

    @Schema(description = "验证码类型，当前为滑块拼图")
    private String captchaType;

    @Schema(description = "登录是否需要滑块验证码")
    private Boolean captchaRequired;

    @Schema(description = "登录配置值集项，键为 ITEM_CODE，值为 ITEM_VALUE")
    private Map<String, String> loginConfig;
}
