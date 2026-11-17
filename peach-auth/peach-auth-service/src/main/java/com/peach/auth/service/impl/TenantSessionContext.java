package com.peach.auth.service.impl;

import org.springframework.util.StringUtils;

/**
 * 当前 token 已通过服务端成员、租户和应用验证的可信租户上下文。
 *
 * <p>所有 ID 都是数据库物理 ID，不使用业务 CODE 作为关联。
 * 机构可以不绑定；成员版本变化后旧 Token 必须被拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 17:35
 */
public record TenantSessionContext(String userRefId, String tenantRefId, String memberRefId,
                                   String appRefId, int year, long memberVersion, long authVersion) {

    /**
     * 检查构造参数是否具备安全的物理关联语义。
     *
     * @param userRefId 用户物理主键
     * @param tenantRefId 租户物理主键
     * @param memberRefId 成员物理主键
     * @param appRefId 应用物理主键
     * @param year 登录选择的自然年度，各年度授权相互隔离
     * @param memberVersion 成员状态版本
     * @param authVersion 角色与资源授权版本
     */
    public TenantSessionContext {
        if (!StringUtils.hasText(userRefId) || !StringUtils.hasText(tenantRefId)
                || !StringUtils.hasText(memberRefId) || !StringUtils.hasText(appRefId)
                || year < 1 || year > 9999 || memberVersion < 0 || authVersion < 0) {
            throw new SecurityException("Invalid tenant session context");
        }
    }
}
