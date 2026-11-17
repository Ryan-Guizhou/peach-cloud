package com.peach.auth.service.impl;

import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.entity.OrganizationDO;
import com.peach.auth.service.TenantMembershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 使用物理父机构引用维护租户内组织树。
 *
 * <p>先锁定启用的租户行，再查询整个目标祖先链，所有同租户移动均按租户串行化，
 * 防止并发交叉移动造成环或跨租户关联。必须由可信管理员权限策略授权；
 * 机构归属不能用来推断管理角色。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 17:10
 */
@Service
@RequiredArgsConstructor
public class OrganizationHierarchyService {

    private static final int MAX_ANCESTOR_DEPTH = 256;
    private static final String ENABLED = "ENABLE";

    private final OrganizationHierarchyDao hierarchyDao;
    private final ObjectProvider<TenantMembershipPolicy> policyProvider;

    /**
     * 将一个机构移动到新父节点，或移动为根节点。重复移动到同一父节点为幂等操作。
     *
     * @param tenantRefId 目标租户物理 ID，不信任浏览器直接声明的租户上下文
     * @param operatorMemberRefId 当前操作者的成员物理 ID
     * @param organizationRefId 待移动的机构物理 ID
     * @param newParentOrgRefId 新父机构物理 ID，null 表示新根
     * @throws SecurityException 未验证租户管理员权限
     * @throws IllegalStateException 机构无效、循环引用、跨租户或并发版本冲突
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void move(String tenantRefId, String operatorMemberRefId,
                     String organizationRefId, String newParentOrgRefId) {
        requireId(tenantRefId, "tenantRefId");
        requireId(operatorMemberRefId, "operatorMemberRefId");
        requireId(organizationRefId, "organizationRefId");
        if (newParentOrgRefId != null) {
            requireId(newParentOrgRefId, "newParentOrgRefId");
        }
        TenantMembershipPolicy policy = policyProvider.getIfAvailable();
        if (policy == null) {
            throw new SecurityException("Tenant hierarchy authorization policy is unavailable");
        }
        policy.requireTenantAdministrator(tenantRefId, operatorMemberRefId);

        String tenantBusinessId = hierarchyDao.lockTenantAndFindBusinessId(tenantRefId);
        if (!StringUtils.hasText(tenantBusinessId)) {
            throw new IllegalStateException("Target tenant is not enabled");
        }
        OrganizationDO organization = activeOrganization(organizationRefId, tenantBusinessId);
        if (Objects.equals(organization.getParentOrgRefId(), newParentOrgRefId)) {
            return;
        }

        Set<String> visited = new HashSet<>();
        visited.add(organizationRefId);
        String currentParentId = newParentOrgRefId;
        while (currentParentId != null) {
            if (!visited.add(currentParentId)) {
                throw new IllegalStateException("Organization hierarchy would contain a cycle");
            }
            if (visited.size() > MAX_ANCESTOR_DEPTH) {
                throw new IllegalStateException("Organization hierarchy exceeds supported depth");
            }
            OrganizationDO parent = activeOrganization(currentParentId, tenantBusinessId);
            currentParentId = parent.getParentOrgRefId();
        }

        Long version = organization.getVersion();
        if (version == null || version < 0) {
            throw new IllegalStateException("Organization version is invalid");
        }
        if (hierarchyDao.updateParent(organizationRefId, tenantBusinessId,
                newParentOrgRefId, version, operatorMemberRefId) != 1) {
            throw new IllegalStateException("Concurrent organization hierarchy change");
        }
    }

    private OrganizationDO activeOrganization(String id, String tenantBusinessId) {
        OrganizationDO organization = hierarchyDao.selectInTenant(id, tenantBusinessId);
        if (organization == null || !ENABLED.equals(organization.getStatus())
                || !Integer.valueOf(0).equals(organization.getIsDelete())) {
            throw new IllegalStateException("Organization is inactive or belongs to another tenant");
        }
        return organization;
    }

    private static void requireId(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
