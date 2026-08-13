package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "个人中心资料")
public class UserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

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
