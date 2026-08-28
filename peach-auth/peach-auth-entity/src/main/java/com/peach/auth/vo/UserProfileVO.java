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

    private String userId;
    private String userCode;
    private String userName;
    private String mobilePhone;
    private String email;
    private String status;
    private String lastestLogin;
    private String defaultOrgId;
    private AvatarHistoryVO currentAvatar;
    private List<AvatarHistoryVO> avatarHistory;
}
