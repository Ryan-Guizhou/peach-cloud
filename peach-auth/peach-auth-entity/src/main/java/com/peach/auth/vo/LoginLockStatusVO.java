package com.peach.auth.vo;

import java.io.Serial;
import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录锁定状态。
 */
@Data
@Schema(description = "登录锁定状态")
public class LoginLockStatusVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否处于锁定状态")
    private boolean locked;

    @Schema(description = "是否永久锁定")
    private boolean permanent;

    @Schema(description = "累计密码错误次数")
    private int failCount;

    @Schema(description = "临时锁定截止时间（毫秒时间戳），永久锁定时为 null")
    private Long lockUntilEpochMs;

    @Schema(description = "剩余锁定秒数，未锁定时为 0")
    private long remainingLockSeconds;
}
