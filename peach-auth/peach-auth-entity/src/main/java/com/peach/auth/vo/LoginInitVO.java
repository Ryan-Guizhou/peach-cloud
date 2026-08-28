package com.peach.auth.vo;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

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
}
