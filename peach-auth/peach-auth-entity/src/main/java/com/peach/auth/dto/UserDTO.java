package com.peach.auth.dto;

import com.peach.auth.group.UserGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户维护请求参数。
 * <p>用于用户基础信息维护和机构关系绑定，不包含登录态和权限结果。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:45
 */
@Data
@Schema(description = "用户DTO")
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 5268266371854905203L;

    @Schema(description = "用户ID")
    @NotBlank(message = "用户ID不能为空", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String userId;

    @Schema(description = "用户账号")
    @NotBlank(message = "用户账号不能为空", groups = {UserGroup.insertGroup.class})
    @Size(min = 4, max = 50, message = "用户账号长度为4-50", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String userCode;

    @Schema(description = "用户密码")
    @NotBlank(message = "用户密码不能为空", groups = {UserGroup.insertGroup.class})
    @Size(min = 6, max = 64, message = "用户密码长度为6-64", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String password;

    @Schema(description = "用户名称")
    @NotBlank(message = "用户名称不能为空", groups = {UserGroup.insertGroup.class})
    @Size(min = 1, max = 50, message = "用户名称长度为1-50", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String userName;

    @Schema(description = "身份证号")
    @Size(max = 20, message = "身份证号长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String identityCode;

    @Schema(description = "密码失效日期")
    @Size(max = 20, message = "密码失效日期长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String invlidate;

    @Schema(description = "认证方式")
    @Size(max = 20, message = "认证方式长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String authMode;

    @Schema(description = "状态")
    @Size(max = 20, message = "状态长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String status;

    @Schema(description = "解锁时间")
    @Size(max = 20, message = "解锁时间长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String unlockTime;

    @Schema(description = "菜单风格")
    @Size(max = 20, message = "菜单风格长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String menuStyle;

    @Schema(description = "菜单角色")
    @Size(max = 20, message = "菜单角色长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String menuRole;

    @Schema(description = "最近登录时间")
    @Size(max = 20, message = "最近登录时间长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String lastestLogin;

    @Schema(description = "密码错误次数")
    @Size(max = 20, message = "密码错误次数长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String errorCount;

    @Schema(description = "有效期开始")
    @Size(max = 20, message = "有效期开始长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String startDate;

    @Schema(description = "有效期结束")
    @Size(max = 20, message = "有效期结束长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String endDate;

    @Schema(description = "手机号")
    @Size(max = 20, message = "手机号长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String mobilePhone;

    @Schema(description = "邮箱")
    @Size(max = 100, message = "邮箱长度不能超过100", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String email;

    @Schema(description = "默认租户ID")
    @Size(max = 32, message = "默认租户ID长度不能超过32", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String defaultTenantId;

    @Schema(description = "默认机构ID")
    @Size(max = 32, message = "默认机构ID长度不能超过32", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String defaultOrgId;

    @Schema(description = "是否删除")
    private Integer isDelete;

    @Schema(description = "是否修改密码")
    private Integer isModify;

    @Schema(description = "密码修改时间")
    @Size(max = 20, message = "密码修改时间长度不能超过20", groups = {UserGroup.insertGroup.class, UserGroup.updateGroup.class})
    private String passwdModifyTime;
}
