package com.peach.auth.entity;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

/**
 * 登录审计记录，仅包含可审计白名单字段，不持有认证凭据。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@Data
public class LoginLogDO {
    /** 日志ID。 */
    private String logId;
    /** 事件时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime occurredAt;
    /** LOGIN、LOGOUT 或 CAPTCHA。 */
    private String eventType;
    /** SUCCESS 或 FAILED。 */
    private String result;
    /** 白名单原因码。 */
    private String reasonCode;
    /** 脱敏账户。 */
    private String account;
    /** 用于精确筛选的账户摘要，不对外返回。 */
    @JsonIgnore
    private String accountHash;
    /** 已认证用户ID。 */
    private String userId;
    /** 仅在认证确认后填写的租户ID。 */
    private String tenantId;
    /** 已确认机构ID。 */
    private String orgId;
    /** 可信来源IP或直连对端地址。 */
    private String ipAddress;
    /** 浏览器和操作系统类别。 */
    private String browser;
    /** 请求耗时毫秒。 */
    private Long durationMs;
    /** 服务端追踪ID。 */
    private String traceId;
}
