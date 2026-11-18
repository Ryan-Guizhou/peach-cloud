package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户管理员邀请已存在的全局用户加入当前租户。
 *
 * <p>目标用户为受邀对象，不允许指定操作者、目标租户和应用。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 18:45
 */
@Data
@Schema(description = "租户成员邀请")
public class SaasMemberInviteDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 219003L;

    @NotBlank(message = "受邀用户物理ID不能为空")
    @Size(max = 32, message = "受邀用户物理ID不能超过32个字符")
    private String inviteeUserRefId;

    @NotNull(message = "邀请有效期不能为空")
    @Future(message = "邀请有效期必须晚于当前时间")
    private LocalDateTime expiresAt;
}
