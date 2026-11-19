package com.peach.auth.service.data;

import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.SaasIamFileQueryScopeVO;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 同一成员角色绑定中的组织范围与数据策略先求交，再跨绑定取并集。
 *
 * <p>返回的是受信 SQL 数据范围，不返回 SQL 字符串；应用端只能在固定 Mapper
 * 的物理租户/应用谓词之后使用本结果中的参数值。机构树不完整时默认拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
public final class FileSaasQueryScopePlanner {

    private static final int MAX_GRANTS = 64;

    private static final int MAX_ORGANIZATIONS = 512;

    private FileSaasQueryScopePlanner() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 构造可安全应用在文件 SELECT/COUNT 的有限行授权范围。
     *
     * @param context Auth 重新核验 Token 得到的可信成员身份
     * @param grants 从真实角色绑定、动作权限及数据策略同表联查的证据
     * @param directory 可信机构树展开器，不能使用前端提交的机构
     * @return 已经求交并并集的只读 SQL 范围
     */
    public static SaasIamFileQueryScopeVO plan(TenantSessionContext context,
                                               List<RoleDataGrantVO> grants,
                                               OrganizationScopeDirectory directory) {
        if (context == null || directory == null || CollectionUtils.isEmpty(grants)
                || grants.size() > MAX_GRANTS) {
            throw new SecurityException("Complete file data policy is required");
        }
        boolean tenantAll = false;
        boolean self = false;
        Set<String> orgs = new TreeSet<>();
        Set<String> selfOrgs = new TreeSet<>();
        for (RoleDataGrantVO grant : grants) {
            if (grant == null || !StringUtils.hasText(grant.getBindingRefId())
                    || !StringUtils.hasText(grant.getRoleRefId())
                    || !StringUtils.hasText(grant.getPermissionRefId())) {
                throw new SecurityException("Invalid file data grant");
            }
            OrgSelection binding = resolveBinding(context.tenantRefId(), grant, directory);
            OrgSelection policy = resolvePolicy(context.tenantRefId(), grant, directory);
            if ("SELF".equals(grant.getDataScope())) {
                if (binding.all()) {
                    self = true;
                } else {
                    selfOrgs.addAll(binding.orgs());
                }
            } else if (binding.all() && policy.all()) {
                tenantAll = true;
            } else if (binding.all()) {
                orgs.addAll(policy.orgs());
            } else if (policy.all()) {
                orgs.addAll(binding.orgs());
            } else {
                Set<String> intersection = new HashSet<>(binding.orgs());
                intersection.retainAll(policy.orgs());
                orgs.addAll(intersection);
            }
            if (orgs.size() > MAX_ORGANIZATIONS || selfOrgs.size() > MAX_ORGANIZATIONS) {
                throw new SecurityException("File organization scope exceeds safe limit");
            }
        }
        if (!tenantAll && !self && orgs.isEmpty() && selfOrgs.isEmpty()) {
            throw new SecurityException("No effective file query scope");
        }
        SaasIamFileQueryScopeVO result = new SaasIamFileQueryScopeVO();
        result.setTenantRefId(context.tenantRefId());
        result.setAppRefId(context.appRefId());
        result.setUserRefId(context.userRefId());
        result.setTenantAll(tenantAll);
        result.setSelf(self);
        result.setOrganizationRefIds(List.copyOf(orgs));
        result.setSelfOrganizationRefIds(List.copyOf(selfOrgs));
        return result;
    }

    private static OrgSelection resolveBinding(String tenantRefId, RoleDataGrantVO grant,
                                               OrganizationScopeDirectory directory) {
        return switch (grant.getBindingScopeType() == null ? "" : grant.getBindingScopeType()) {
            case "TENANT" -> unrestricted(grant.getBindingOrgRefId());
            case "ORG" -> exact(tenantRefId, grant.getBindingOrgRefId(), directory);
            case "ORG_TREE" -> descendants(tenantRefId, grant.getBindingOrgRefId(), directory);
            default -> throw new SecurityException("Unrecognized member-role binding scope");
        };
    }

    private static OrgSelection resolvePolicy(String tenantRefId, RoleDataGrantVO grant,
                                              OrganizationScopeDirectory directory) {
        return switch (grant.getDataScope() == null ? "" : grant.getDataScope()) {
            case "SELF", "TENANT_ALL" -> unrestricted(grant.getDataOrgRefId());
            case "ORG" -> exact(tenantRefId, grant.getDataOrgRefId(), directory);
            case "ORG_TREE" -> descendants(tenantRefId, grant.getDataOrgRefId(), directory);
            default -> throw new SecurityException("Unrecognized file data policy");
        };
    }

    private static OrgSelection unrestricted(String anchor) {
        if (StringUtils.hasText(anchor)) {
            throw new SecurityException("Tenant-wide scope cannot specify an organization");
        }
        return new OrgSelection(true, Set.of());
    }

    private static OrgSelection exact(String tenantRefId, String anchor,
                                      OrganizationScopeDirectory directory) {
        if (!StringUtils.hasText(anchor)
                || !directory.contains(tenantRefId, anchor, anchor)) {
            throw new SecurityException("File organization scope is inactive");
        }
        return new OrgSelection(false, Set.of(anchor));
    }

    private static OrgSelection descendants(String tenantRefId, String anchor,
                                            OrganizationScopeDirectory directory) {
        if (!StringUtils.hasText(anchor)) {
            throw new SecurityException("Organization tree anchor is required");
        }
        Set<String> ids = directory.descendants(tenantRefId, anchor);
        if (ids == null || ids.isEmpty() || ids.size() > MAX_ORGANIZATIONS
                || !ids.contains(anchor) || ids.stream().anyMatch(id ->
                !StringUtils.hasText(id) || id.length() > 32)) {
            throw new SecurityException("Incomplete organization scope");
        }
        return new OrgSelection(false, Set.copyOf(ids));
    }

    private record OrgSelection(boolean all, Set<String> orgs) {
    }
}
