package com.peach.auth.service.data;

import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.vo.RoleDataGrantVO;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 业务 SQL 行级过滤的可信、结构化参数，禁止直接拼接成 SQL 字符串。
 *
 * <p>每个 Clause 同时包含同一条角色绑定与同一数据策略的交集；
 * 不同 Clause 仅在业务 SQL 中以 OR 组合。调用方必须在执行 COUNT、
 * SELECT、EXPORT、UPDATE/DELETE 之前，以 MyBatis 参数绑定同时强制
 * tenantRefId、appRefId、IS_DELETE 和每条 Clause，不能取数后过滤。
 * 当前对象的 org/owner 列必须由业务表提供，不得从请求传入。
 * 无匹配授权时创建失败，不存在隐式租户全量回退。</p>
 *
 * @param tenantRefId 可信租户物理主键
 * @param appRefId 可信应用物理主键
 * @param clauses 按完整独立角色绑定形成的安全查询限制
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:20
 */
public record AuthorizedDataQueryScope(String tenantRefId, String appRefId,
                                       List<RowClause> clauses) {

    private static final int MAX_GRANTS = 128;

    public AuthorizedDataQueryScope {
        if (!StringUtils.hasText(tenantRefId) || !StringUtils.hasText(appRefId) || CollectionUtils.isEmpty(clauses)) {
            throw new SecurityException("Verified tenant query scope is required");
        }
        clauses = List.copyOf(clauses);
    }

    /**
     * 由已在 DAO 中通过成员、角色、资源和动作联合校验的证据构造授权计划。
     * 仅可在重新验证的当前 Token 上下文中调用；机构展开严格有界。
     *
     * @param context 重新验证的当前成员 Session
     * @param grants 同一动作由数据库查询的完整绑定证据
     * @param directory 可信机构目录
     * @return 安全预筛选 SQL 参数，空权限拒绝
     */
    public static AuthorizedDataQueryScope from(TenantSessionContext context,
                                                List<RoleDataGrantVO> grants,
                                                OrganizationScopeDirectory directory) {
        if (context == null || !StringUtils.hasText(context.userRefId())
                || !StringUtils.hasText(context.tenantRefId())
                || !StringUtils.hasText(context.appRefId()) || directory == null
                || CollectionUtils.isEmpty(grants) || grants.size() > MAX_GRANTS) {
            throw new SecurityException("Usable row-level grants are required");
        }
        List<RowClause> clauses = new ArrayList<>();
        Map<String, Set<String>> trees = new HashMap<>();
        Map<String, Boolean> activeOrganizations = new HashMap<>();
        for (RoleDataGrantVO grant : grants) {
            if (grant == null || !StringUtils.hasText(grant.getBindingRefId())
                    || !StringUtils.hasText(grant.getRoleRefId())
                    || !StringUtils.hasText(grant.getPermissionRefId())) {
                continue;
            }
            Set<String> binding = organizations(grant.getBindingScopeType(), grant.getBindingOrgRefId(),
                    false, context.tenantRefId(), directory, trees, activeOrganizations);
            Set<String> policy = organizations(grant.getDataScope(), grant.getDataOrgRefId(),
                    true, context.tenantRefId(), directory, trees, activeOrganizations);
            // null 表示不限制组织；空集合表示非法/不可用机构，不可放宽为 TENANT_ALL。
            if ((binding != null && binding.isEmpty()) || (policy != null && policy.isEmpty())) {
                continue;
            }
            Set<String> intersection = intersect(binding, policy);
            if (intersection != null && intersection.isEmpty()) {
                continue;
            }
            String owner = "SELF".equals(grant.getDataScope()) ? context.userRefId() : null;
            clauses.add(new RowClause(intersection != null,
                    intersection == null ? List.of() : List.copyOf(intersection), owner,
                    grant.getBindingRefId(), grant.getRoleRefId(), grant.getPermissionRefId()));
        }
        return new AuthorizedDataQueryScope(context.tenantRefId(), context.appRefId(), clauses);
    }

    private static Set<String> organizations(String type, String anchor, boolean policy,
                                             String tenantRefId, OrganizationScopeDirectory directory,
                                             Map<String, Set<String>> trees,
                                             Map<String, Boolean> activeOrganizations) {
        if (!policy && "TENANT".equals(type) && !StringUtils.hasText(anchor)) {
            return null;
        }
        if (policy && ("TENANT_ALL".equals(type) || "SELF".equals(type))
                && !StringUtils.hasText(anchor)) {
            return null;
        }
        if (!StringUtils.hasText(anchor)) {
            return Set.of();
        }
        if ("ORG".equals(type)) {
            boolean active = activeOrganizations.computeIfAbsent(anchor,
                    ref -> directory.contains(tenantRefId, ref, ref));
            return active ? Set.of(anchor) : Set.of();
        }
        if ("ORG_TREE".equals(type)) {
            return trees.computeIfAbsent(anchor, ref -> directory.descendants(tenantRefId, ref));
        }
        return Set.of();
    }

    private static Set<String> intersect(Set<String> left, Set<String> right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        Set<String> result = new HashSet<>(left);
        result.retainAll(right);
        return Set.copyOf(result);
    }

    /**
     * 一个独立授权对应一组必须同时满足的数据库行谓词。
     *
     * <p>organizationRequired=false 表示当前证据不限制机构，不能对整个计划生效。
     * requiredOwnerUserRefId 非空表示必须同时匹配所有者；绝不可在不同 Clause 之间
     * 分别组合 owner 与 organization。</p>
     */
    public record RowClause(boolean organizationRequired, List<String> allowedOrgRefIds,
                            String requiredOwnerUserRefId, String bindingRefId,
                            String roleRefId, String permissionRefId) {
        public RowClause {
            Objects.requireNonNull(allowedOrgRefIds, "Allowed organizations are required");
            allowedOrgRefIds = List.copyOf(allowedOrgRefIds);
            if (organizationRequired && allowedOrgRefIds.isEmpty()) {
                throw new SecurityException("Restricted organization scope cannot be empty");
            }
            if (!organizationRequired && !allowedOrgRefIds.isEmpty()) {
                throw new SecurityException("Unrestricted organization scope cannot carry anchors");
            }
            if (!StringUtils.hasText(bindingRefId) || !StringUtils.hasText(roleRefId)
                    || !StringUtils.hasText(permissionRefId)) {
                throw new SecurityException("Role-binding evidence is incomplete");
            }
        }
    }
}
