package com.peach.auth.entity;

import java.io.Serial;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * User实体。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
  */
@Data
@Entity
@Table(name = "PEACH_USER")
@Schema(description = "User实体")
@EqualsAndHashCode(callSuper = true)
public class UserDO extends PeachDO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3354777952204829102L;

    @Id
    @Column(name = "USER_ID")
    @Schema(description = "用户ID")
    private String userId;

    @Column(name = "USER_CODE")
    @Schema(description = "用户账号")
    private String userCode;

    @Column(name = "PASSWORD")
    @Schema(description = "用户密码")
    private String password;

    @Column(name = "USER_NAME")
    @Schema(description = "用户名称")
    private String userName;

    @Column(name = "IDENTITY_CODE")
    @Schema(description = "用户身份证号")
    private String identityCode;

    @Column(name = "INVLIDATE")
    @Schema(description = "密码失效日期")
    private String invlidate;

    @Column(name = "AUTH_MODE")
    @Schema(description = "认证方式")
    private String authMode;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private String status;

    @Column(name = "UNLOCK_TIME")
    @Schema(description = "解锁时间")
    private String unlockTime;

    @Column(name = "MENU_STYLE")
    @Schema(description = "菜单风格")
    private String menuStyle;

    @Column(name = "MENU_ROLE")
    @Schema(description = "菜单角色")
    private String menuRole;

    @Column(name = "LASTEST_LOGIN")
    @Schema(description = "最近登录时间")
    private String lastestLogin;

    @Column(name = "ERROR_COUNT")
    @Schema(description = "密码错误次数")
    private String errorCount;

    @Column(name = "START_DATE")
    @Schema(description = "有效期开始")
    private String startDate;

    @Column(name = "END_DATE")
    @Schema(description = "有效期结束")
    private String endDate;

    @Column(name = "MOBILE_PHONE")
    @Schema(description = "手机号")
    private String mobilePhone;

    @Column(name = "EMAIL")
    @Schema(description = "邮箱")
    private String email;

    @Column(name = "DEFAULT_ORG_ID")
    @Schema(description = "默认机构ID")
    private String defaultOrgId;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;

    @Column(name = "IS_MODIFY")
    @Schema(description = "密码是否修改")
    private Integer isModify;

    @Column(name = "PASSWD_MODIFY_TIME")
    @Schema(description = "密码修改时间")
    private String passwdModifyTime;

}
