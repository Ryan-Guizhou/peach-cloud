package com.peach.userservice.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Version;
import java.io.Serializable;

/**
 * 用户实体类
 * 对应数据库表：PEACH_USER
 */
@Data
@Entity
@Table(name = "PEACH_USER")
@Schema(description = "用户实体")
public class UserDO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ================= 主键与基本信息 =================

    @Id
    @Column(name = "ID", nullable = false, length = 36, updatable = false)
    @Schema(
            description = "用户ID，UUID格式",
            example = "123e4567-e89b-12d3-a456-426614174000",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String id;

    @Column(name = "USERNAME", nullable = false, length = 64, unique = true)
    @Schema(
            description = "用户名，登录账号",
            example = "zhangsan",
            required = true
    )
    private String username;

    @Column(name = "NICKNAME", nullable = false, length = 128)
    @Schema(
            description = "用户昵称，显示名称",
            example = "张三",
            required = true
    )
    private String nickname;

    @Column(name = "REAL_NAME", length = 64)
    @Schema(
            description = "真实姓名",
            example = "张三"
    )
    private String realName;

    // ================= 联系信息 =================

    @Column(name = "EMAIL", length = 128, unique = true)
    @Schema(
            description = "电子邮箱",
            example = "zhangsan@example.com"
    )
    private String email;

    @Column(name = "PHONE", length = 20, unique = true)
    @Schema(
            description = "手机号，国际格式",
            example = "+8613800138000"
    )
    private String phone;

    @Column(name = "PHONE_VERIFIED", nullable = false)
    @Schema(
            description = "手机号是否已验证：0-未验证，1-已验证",
            example = "0",
            defaultValue = "0"
    )
    private Boolean phoneVerified = false;

    @Column(name = "EMAIL_VERIFIED", nullable = false)
    @Schema(
            description = "邮箱是否已验证：0-未验证，1-已验证",
            example = "0",
            defaultValue = "0"
    )
    private Boolean emailVerified = false;

    @Column(name = "AVATAR_URL", length = 512)
    @Schema(
            description = "头像URL地址",
            example = "https://example.com/avatar.jpg"
    )
    private String avatarUrl;

    @Column(name = "PASSWORD_UPDATED_AT")
    @Schema(
            description = "密码最后修改时间",
            example = "2024-01-15T10:30:00"
    )
    private String passwordUpdatedAt;

    @Column(name = "LAST_LOGIN_IP", length = 45)
    @Schema(
            description = "最后登录IP地址",
            example = "192.168.1.1"
    )
    private String lastLoginIp;

    @Column(name = "LAST_LOGIN_TIME")
    @Schema(
            description = "最后登录时间",
            example = "2024-01-15T10:30:00"
    )
    private String lastLoginTime;

    @Column(name = "LAST_LOGIN_DEVICE", length = 255)
    @Schema(
            description = "最后登录设备信息",
            example = "Chrome/120.0.0.0 on Windows 10"
    )
    private String lastLoginDevice;

    @Column(name = "LOGIN_FAIL_COUNT", nullable = false)
    @Schema(
            description = "连续登录失败次数",
            example = "0",
            defaultValue = "0"
    )
    private Integer loginFailCount = 0;

    @Column(name = "LOGIN_LOCK_UNTIL")
    @Schema(
            description = "登录锁定截止时间",
            example = "2024-01-15T11:30:00"
    )
    private String loginLockUntil;

    // ================= 账户状态 =================

    @Column(name = "ACCOUNT_STATUS", nullable = false)
    @Schema(
            description = "账户状态：0-禁用，1-启用，2-锁定，3-过期，4-待激活",
            example = "1",
            defaultValue = "1",
            allowableValues = {"0", "1", "2", "3", "4"}
    )
    private Integer accountStatus = 1;

    @Column(name = "ENABLED", nullable = false)
    @Schema(
            description = "是否启用：0-禁用，1-启用",
            example = "1",
            defaultValue = "1"
    )
    private Boolean enabled = true;

    @Column(name = "LOCKED", nullable = false)
    @Schema(
            description = "是否锁定：0-否，1-是",
            example = "0",
            defaultValue = "0"
    )
    private Boolean locked = false;

    // ================= 个人信息 =================

    @Column(name = "GENDER")
    @Schema(
            description = "性别：0-未知，1-男，2-女",
            example = "1",
            allowableValues = {"0", "1", "2"}
    )
    private Integer gender;

    @Column(name = "BIRTHDAY")
    @Schema(
            description = "出生日期",
            example = "1990-01-01"
    )
    private String birthday;

    @Column(name = "COUNTRY", length = 64)
    @Schema(
            description = "国家",
            example = "中国"
    )
    private String country;

    @Column(name = "PROVINCE", length = 64)
    @Schema(
            description = "省份",
            example = "北京市"
    )
    private String province;

    @Column(name = "CITY", length = 64)
    @Schema(
            description = "城市",
            example = "北京市"
    )
    private String city;

    @Column(name = "ADDRESS", length = 512)
    @Schema(
            description = "详细地址",
            example = "朝阳区建国门外大街1号"
    )
    private String address;

    // ================= 系统信息 =================

    @Column(name = "SOURCE_TYPE", nullable = false)
    @Schema(
            description = "用户来源：1-自主注册，2-后台创建，3-第三方导入",
            example = "1",
            defaultValue = "1",
            allowableValues = {"1", "2", "3"}
    )
    private Integer sourceType = 1;

    @Column(name = "SOURCE_CHANNEL", length = 64)
    @Schema(
            description = "来源渠道：web, app, wechat等",
            example = "web"
    )
    private String sourceChannel;

    @Column(name = "INVITE_CODE", length = 32)
    @Schema(
            description = "邀请码",
            example = "PEACH2024"
    )
    private String inviteCode;

    @Column(name = "INVITED_BY")
    @Schema(
            description = "邀请人ID",
            example = "123e4567-e89b-12d3-a456-426614174001"
    )
    private String invitedBy;

    @Lob
    @Column(name = "REMARK", columnDefinition = "TEXT")
    @Schema(
            description = "管理员备注"
    )
    private String remark;

    // ================= 时间戳 =================

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    @Schema(
            description = "创建时间",
            example = "2024-01-15T10:30:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    @Schema(
            description = "更新时间",
            example = "2024-01-15T10:30:00",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private String updatedAt;

    @Version
    @Column(name = "VERSION", nullable = false)
    @Schema(
            description = "版本号，用于乐观锁",
            example = "1",
            defaultValue = "1",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    private Integer version = 1;

    public static void main(String[] args) {

    }
}