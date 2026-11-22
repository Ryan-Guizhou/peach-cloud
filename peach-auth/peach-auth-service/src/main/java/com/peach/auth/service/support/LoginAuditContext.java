package com.peach.auth.service.support;

import com.peach.auth.entity.LoginLogDO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * 单次认证请求的最小审计上下文；由过滤器创建并在 finally 清理，不持有请求或凭据。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
public final class LoginAuditContext {
    private static final ThreadLocal<LoginLogDO> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Long> STARTED = new ThreadLocal<>();
    private LoginAuditContext() { }

    /** 创建请求范围记录；默认视为未完成认证，只有明确成功时更新结果。 */
    public static void begin(String event, String ip, String browser, String trace) {
        LoginLogDO row = new LoginLogDO();
        row.setLogId(UUID.randomUUID().toString().replace("-", ""));
        row.setOccurredAt(LocalDateTime.now()); row.setEventType(event);
        row.setResult("FAILED"); row.setReasonCode("REQUEST_REJECTED");
        row.setIpAddress(ip); row.setBrowser(browser); row.setTraceId(trace);
        CURRENT.set(row); STARTED.set(System.nanoTime());
    }
    /** 仅保存脱敏账户及精确查询摘要；限制长度并移除控制字符。 */
    public static void account(String value) {
        LoginLogDO row = CURRENT.get();
        if (row == null || value == null) return;
        String account = value.replaceAll("[\\p{Cntrl}]", "").trim();
        if (account.length() > 128) account = account.substring(0, 128);
        row.setAccount(account.length() < 3 ? "***" : account.substring(0, 2) + "***" + account.substring(account.length() - 1));
        row.setAccountHash(hashAccount(account));
    }
    /** 账户摘要只用于精确筛选，不作为认证证据。 */
    public static String hashAccount(String account) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(account.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable", ex); }
    }
    /** 仅允许调用方传入固定原因码，禁止异常内容和用户输入。 */
    public static void reason(String code) { if (CURRENT.get() != null) CURRENT.get().setReasonCode(code); }
    /** 身份归属只能在认证或已有会话验证后填写。 */
    public static void identity(String user, String tenant, String org) {
        LoginLogDO row = CURRENT.get();
        if (row != null) { row.setUserId(user); row.setTenantId(tenant); row.setOrgId(org); }
    }
    /** 只在认证入口明确成功后调用。 */
    public static void success() {
        if (CURRENT.get() != null) { CURRENT.get().setResult("SUCCESS"); CURRENT.get().setReasonCode("OK"); }
    }
    /** 结束计时，不清理上下文；调用方必须在 finally 调用 clear。 */
    public static LoginLogDO finish() {
        LoginLogDO row = CURRENT.get();
        if (row != null && STARTED.get() != null) row.setDurationMs(Math.max(0, (System.nanoTime() - STARTED.get()) / 1_000_000));
        return row;
    }
    /** 线程复用前清除记录，防止身份串联。 */
    public static void clear() { CURRENT.remove(); STARTED.remove(); }
}
