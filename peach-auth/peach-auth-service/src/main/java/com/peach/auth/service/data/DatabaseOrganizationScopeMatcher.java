package com.peach.auth.service.data;

import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.entity.OrganizationDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 只查询有效租户和机构的父机构链来判定 ORG_TREE。
 *
 * <p>以服务端物理租户 ID 查找租户业务号，然后只在该租户查询有效机构。
 * 任何组织被删除、停用、链路断开、环路或层级过深均拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
@Service
@RequiredArgsConstructor
public class DatabaseOrganizationScopeMatcher implements OrganizationScopeDirectory {

    private static final int MAX_ANCESTORS = 256;
    private static final int MAX_QUERY_ORGANIZATIONS = 512;
    private static final String ENABLED = "ENABLE";

    private final OrganizationHierarchyDao organizationHierarchyDao;

    /**
     * 在执行业务 SQL 之前解析有界的有效机构子树。
     *
     * <p>任何越界、重复节点、缺失锚点或已停用租户均拒绝整个授权计划，
     * 不会把空集合当成整个租户可见。</p>
     */
    @Override
    @Transactional(readOnly = true)
    public Set<String> descendants(String tenantRefId, String ancestorOrgRefId) {
        if (!StringUtils.hasText(tenantRefId) || !StringUtils.hasText(ancestorOrgRefId)) {
            throw new SecurityException("Active tenant and organization are required");
        }
        String tenantBusinessId = organizationHierarchyDao.findEnabledTenantBusinessId(tenantRefId);
        if (!StringUtils.hasText(tenantBusinessId)) {
            throw new SecurityException("Tenant is not active");
        }
        List<String> ids = organizationHierarchyDao.selectActiveDescendantIds(
                tenantBusinessId, ancestorOrgRefId, MAX_ANCESTORS, MAX_QUERY_ORGANIZATIONS + 1);
        if (ids == null || ids.isEmpty() || ids.size() > MAX_QUERY_ORGANIZATIONS
                || !ancestorOrgRefId.equals(ids.get(0))
                || ids.stream().anyMatch(id -> !StringUtils.hasText(id))) {
            throw new SecurityException("Organization subtree cannot be safely enumerated");
        }
        Set<String> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new SecurityException("Organization subtree contains a cycle");
        }
        return Set.copyOf(unique);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contains(String tenantRefId, String ancestorOrgRefId, String candidateOrgRefId) {
        if (!StringUtils.hasText(tenantRefId) || !StringUtils.hasText(ancestorOrgRefId)
                || !StringUtils.hasText(candidateOrgRefId)) {
            return false;
        }
        String tenantBusinessId = organizationHierarchyDao.findEnabledTenantBusinessId(tenantRefId);
        if (!StringUtils.hasText(tenantBusinessId)) {
            return false;
        }
        String currentId = candidateOrgRefId;
        Set<String> visited = new HashSet<>();
        while (StringUtils.hasText(currentId) && visited.size() < MAX_ANCESTORS
                && visited.add(currentId)) {
            OrganizationDO current = organizationHierarchyDao.selectInTenant(currentId, tenantBusinessId);
            if (current == null || !ENABLED.equals(current.getStatus())
                    || !Integer.valueOf(0).equals(current.getIsDelete())) {
                return false;
            }
            if (ancestorOrgRefId.equals(current.getId())) {
                return true;
            }
            currentId = current.getParentOrgRefId();
        }
        return false;
    }
}
