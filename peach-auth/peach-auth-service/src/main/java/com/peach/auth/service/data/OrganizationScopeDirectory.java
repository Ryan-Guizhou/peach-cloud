package com.peach.auth.service.data;

import java.util.Set;

/**
 * 受信机构目录，支持单对象祖先判断和 SQL 查询前的有界机构展开。
 *
 * <p>查询侧必须拒绝无法完整列举的子树，不能以遗漏范围替代安全验收。
 * 不允许从客户端传入租户上下文。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:20
 */
public interface OrganizationScopeDirectory extends OrganizationScopeMatcher {

    /**
     * 获取有效租户内指定机构及其有效子孙机构物理主键。
     *
     * @param tenantRefId 当前可信租户物理主键
     * @param ancestorOrgRefId 有效根机构物理主键
     * @return 不可变机构主键集合，失败时抛出 SecurityException
     */
    Set<String> descendants(String tenantRefId, String ancestorOrgRefId);
}
