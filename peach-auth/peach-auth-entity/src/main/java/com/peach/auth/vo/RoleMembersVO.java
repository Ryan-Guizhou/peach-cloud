package com.peach.auth.vo;

import lombok.Data;
import java.util.List;

/**
 * 当前角色成员与可分配的本机构用户，不包含其他角色的成员关系。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@Data
public class RoleMembersVO {
    private List<UserVO> users;
    private List<String> userCodes;
    private String revision;
}
