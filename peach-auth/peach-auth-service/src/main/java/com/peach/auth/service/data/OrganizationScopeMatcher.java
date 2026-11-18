package com.peach.auth.service.data;

/**
 * 授权查询层的可信机构祖先链判断扩展点。
 *
 * <p>返回 true 当且仅当目标机构有效、属于可信租户、且为指定祖先本身或其后代。
 * 数据库异常必须拒绝授权，不允许因机构服务失败自动放宽权限。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
@FunctionalInterface
public interface OrganizationScopeMatcher {

    /**
     * 判断某有效机构是否属于指定祖先的机构树。
     *
     * @param tenantRefId 物理租户主键
     * @param ancestorOrgRefId 机构祖先物理主键
     * @param candidateOrgRefId 数据对象机构物理主键
     * @return 机构树包含时才允许
     */
    boolean contains(String tenantRefId, String ancestorOrgRefId, String candidateOrgRefId);
}
