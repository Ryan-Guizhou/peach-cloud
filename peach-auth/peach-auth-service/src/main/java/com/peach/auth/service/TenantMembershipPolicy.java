package com.peach.auth.service;

/**
 * 租户成员变更的可信身份与管理授权策略。
 *
 * <p>认证和权限引擎必须实现该接口并从可信 Token/服务上下文校验身份；
 * 禁止根据浏览器传入的 USER_REF_ID 或 MEMBER_REF_ID 自行声明权限。
 * 没有安装策略时，成员服务对所有变更默认拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 16:40
 */
public interface TenantMembershipPolicy {

    /**
     * 确认当前可信身份就是指定的全局用户物理主键。
     *
     * @param userRefId 全局用户物理主键
     * @throws SecurityException 未登录或身份不匹配时
     */
    void requireCurrentUser(String userRefId);

    /**
     * 确认当前可信身份具有该租户的成员管理授权。
     *
     * @param tenantRefId 租户物理主键
     * @param operatorMemberRefId 当前操作者在租户内的成员物理主键
     * @throws SecurityException 不具备授权、身份不匹配或跨租户时
     */
    void requireTenantAdministrator(String tenantRefId, String operatorMemberRefId);

    /**
     * 在服务端 Token 验证后要求操作者具备指定租户级语义权限。
     *
     * @param tenantRefId 租户物理主键
     * @param operatorMemberRefId 操作者成员物理主键
     * @param permissionCode 固定权限语义码
     * @throws SecurityException 任一身份、租户、年度或授权范围不匹配时
     */
    void requireTenantPermission(String tenantRefId, String operatorMemberRefId,
                                 String permissionCode);
}
