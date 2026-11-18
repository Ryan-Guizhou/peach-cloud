package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 授予当前租户和应用内某成员的一条独立角色绑定。
 *
 * <p>租户、应用和操作人都从已认证 Token 获取，不能被客户端覆盖。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Data
@Schema(description = "成员独立角色范围授权")
public class SaasMemberRoleGrantDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 147456204295001136L;

    @NotBlank(message = "目标成员ID不能为空")
    @Size(max = 32, message = "目标成员ID不能超过32个字符")
    private String targetMemberRefId;

    @NotBlank(message = "目标角色ID不能为空")
    @Size(max = 32, message = "目标角色ID不能超过32个字符")
    private String roleRefId;

    @NotBlank(message = "范围类型不能为空")
    @Size(max = 16, message = "范围类型不能超过16个字符")
    private String scopeType;

    @Size(max = 32, message = "范围机构ID不能超过32个字符")
    private String scopeRefId;
}
