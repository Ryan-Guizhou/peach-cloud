package com.peach.auth.vo;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 个人中心资料。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Data
@Schema(description = "个人中心资料")
public class UserProfileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -7169424813458035851L;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "手机号")
    private String mobilePhone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "用户状态")
    private String status;

    @Schema(description = "最近登录时间")
    private String lastestLogin;

    @Schema(description = "默认组织ID")
    private String defaultOrgId;

    @Schema(description = "当前头像")
    private AvatarHistoryVO currentAvatar;

    @Schema(description = "头像历史")
    private List<AvatarHistoryVO> avatarHistory;
}
