package com.peach.auth.qo;

import com.peach.common.PeachEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 登录日志只读查询；租户与账户摘要由服务端计算。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQO extends PeachEntity {
    @Pattern(regexp = "TENANT|UNASSIGNED")
    private String scope = "TENANT";
    @Size(max = 128)
    private String account;
    @Pattern(regexp = "SUCCESS|FAILED")
    private String result;
    @Pattern(regexp = "LOGIN|LOGOUT|CAPTCHA")
    private String eventType;
    @Pattern(regexp = "[0-9a-fA-F:.]{1,45}")
    private String ipAddress;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    @JsonIgnore
    private String tenantId;
    @JsonIgnore
    private String accountHash;
    @Override @NotNull @Min(1)
    public Integer getPageNum() { return super.getPageNum(); }
    @Override @NotNull @Min(1) @Max(100)
    public Integer getPageSize() { return super.getPageSize(); }
}
