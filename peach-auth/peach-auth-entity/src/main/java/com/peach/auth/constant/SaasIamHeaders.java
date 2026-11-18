package com.peach.auth.constant;

/**
 * 仅用于可信服务间传递当前最终用户 Token 的专用头。
 *
 * <p>必须与独立 Same-Token 服务身份核验结合，不得记录明文，不得向下游
 * 业务日志、浏览器响应或消息队列写入；不可从请求体中的租户字段推断身份。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:20
 */
public final class SaasIamHeaders {

    public static final String SUBJECT_TOKEN = "X-Peach-Subject-Token";

    private SaasIamHeaders() {
        throw new IllegalStateException("Utility class");
    }
}
