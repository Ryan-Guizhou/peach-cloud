package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.dao.ScopedDataPolicyManagementDao;
import com.peach.auth.dao.ScopedRoleManagementDao;
import com.peach.auth.dto.SaasDataPolicyUpdateDTO;
import com.peach.auth.dto.SaasFieldPolicyUpdateDTO;
import com.peach.auth.entity.OrganizationDO;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.IamPermissionCodes;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.RoleDataPolicyVO;
import com.peach.auth.vo.RoleFieldPolicyVO;
import com.peach.auth.vo.RolePermissionGrantVO;
import com.peach.common.unique.UniqueIdFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SaaS 角色动作行级和字段级授权策略的安全管理事务。
 *
 * <p>操作人仅从当前 Sa-Token 获取，使用租户锁及角色行锁串行化写入。
 * 必须拥有完整租户角色管理权限、待授权动作的完整租户权限；
 * SYSTEM 动作还要求已经持有该动作的有效 TENANT + TENANT_ALL 行级证据。
 * 数据库有效目录和可委派性检查均失败即拒绝，不允许自报租户/年度或越权
 * 修改平台系统角色。策略变化、旧 Token 失效和审计处于同一事务。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:45
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaasPolicyManagementService {

    private static final int MAX_DATA_RULES = 32;
    private static final int MAX_FIELD_RULES = 128;
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_.-]{0,63}$");
    private static final String ROLE_TENANT = "TENANT";
    private static final String OWNER_TENANT = "TENANT";
    private static final String OWNER_SYSTEM = "SYSTEM";

    private final TenantSessionService sessionService;
    private final TenantMembershipPolicy membershipPolicy;
    private final ScopedRoleManagementDao roleDao;
    private final ScopedDataAuthorizationDao authorizationDao;
    private final ScopedDataPolicyManagementDao policyDao;
    private final ScopedPermissionService permissionService;
    private final OrganizationHierarchyDao hierarchyDao;
    private final MemberAuthVersionDao versionDao;
    private final IamAuthorizationAuditDao auditDao;

    /**
     * 事务内替换角色某一动作的完整行范围定义。
     *
     * <p>删除全部规则是明确撤销，不能通过 null 请求绕过。</p>
     *
     * @param roleRefId 租户年度角色物理主键
     * @param permissionRefId 已授予该角色的权限物理主键
     * @param proposed 新的完整策略列表
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void replaceDataPolicies(String roleRefId, String permissionRefId,
                                    List<SaasDataPolicyUpdateDTO.Rule> proposed) {
        requireCount(proposed, MAX_DATA_RULES);
        Guard guard = lockGuard(roleRefId, permissionRefId);
        Set<DataRule> next = validateDataRules(guard, proposed);
        List<RoleDataPolicyVO> stored = policyDao.selectDataPolicies(
                guard.context.tenantRefId(), guard.context.appRefId(), roleRefId, permissionRefId);
        Set<DataRule> previous = new HashSet<>();
        if (!CollectionUtils.isEmpty(stored)) {
            for (RoleDataPolicyVO row : stored) {
                previous.add(new DataRule(row.getDataScope(), row.getOrgRefId()));
            }
        }
        if (previous.equals(next)) {
            return;
        }
        if (policyDao.retireDataPolicies(guard.context.tenantRefId(), guard.context.appRefId(),
                roleRefId, permissionRefId) != (stored == null ? 0 : stored.size())) {
            throw new IllegalStateException("Data policy update conflicted");
        }
        for (DataRule rule : next) {
            if (policyDao.insertDataPolicy(UniqueIdFacade.nextId(), guard.context.tenantRefId(),
                    guard.context.appRefId(), roleRefId, permissionRefId,
                    rule.scope, rule.organization, guard.context.memberRefId()) != 1) {
                throw new IllegalStateException("Data policy creation failed");
            }
        }
        invalidateAndAudit(guard, roleRefId, "DATA_POLICY_REPLACE");
        log.info("Role data policy updated, tenantRefId={}, roleRefId={}",
                guard.context.tenantRefId(), roleRefId);
    }

    /**
     * 事务内替换某一受控资源字段的完整读写策略；未列出的字段始终默认拒绝。
     *
     * @param roleRefId 租户年度角色物理主键
     * @param permissionRefId 已授予该角色的动作权限物理主键
     * @param resourceCode 受控资源的稳定标识，不参与拼接 SQL
     * @param proposed 新的完整字段策略列表
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void replaceFieldPolicies(String roleRefId, String permissionRefId,
                                     String resourceCode, List<SaasFieldPolicyUpdateDTO.Rule> proposed) {
        requireCount(proposed, MAX_FIELD_RULES);
        if (!StringUtils.hasText(resourceCode)
                || !RESOURCE_PATTERN.matcher(resourceCode).matches()) {
            throw new IllegalArgumentException("Invalid resource code");
        }
        Guard guard = lockGuard(roleRefId, permissionRefId);
        Set<FieldRule> next = validateFieldRules(guard, resourceCode, proposed);
        List<RoleFieldPolicyVO> stored = policyDao.selectFieldPolicies(
                guard.context.tenantRefId(), guard.context.appRefId(), roleRefId, permissionRefId);
        Set<FieldRule> previous = new HashSet<>();
        if (!CollectionUtils.isEmpty(stored)) {
            for (RoleFieldPolicyVO row : stored) {
                previous.add(new FieldRule(row.getFieldRefId(), row.getReadEffect(), row.getWriteEffect()));
            }
        }
        if (previous.equals(next)) {
            return;
        }
        if (policyDao.retireFieldPolicies(guard.context.tenantRefId(), guard.context.appRefId(),
                roleRefId, permissionRefId) != (stored == null ? 0 : stored.size())) {
            throw new IllegalStateException("Field policy update conflicted");
        }
        for (FieldRule rule : next) {
            if (policyDao.insertFieldPolicy(UniqueIdFacade.nextId(), guard.context.tenantRefId(),
                    guard.context.appRefId(), roleRefId, permissionRefId,
                    rule.fieldRefId, rule.readEffect, rule.writeEffect,
                    guard.context.memberRefId()) != 1) {
                throw new IllegalStateException("Field policy creation failed");
            }
        }
        invalidateAndAudit(guard, roleRefId, "FIELD_POLICY_REPLACE");
        log.info("Role field policy updated, tenantRefId={}, roleRefId={}",
                guard.context.tenantRefId(), roleRefId);
    }

    private Guard lockGuard(String roleRefId, String permissionRefId) {
        if (!StringUtils.hasText(roleRefId) || !StringUtils.hasText(permissionRefId)) {
            throw new IllegalArgumentException("Role and permission IDs are required");
        }
        TenantSessionContext context = sessionService.requireCurrent();
        membershipPolicy.requireTenantPermission(context.tenantRefId(), context.memberRefId(),
                IamPermissionCodes.TENANT_ROLE_MANAGE);
        String tenantBusinessId = hierarchyDao.lockTenantAndFindBusinessId(context.tenantRefId());
        if (!StringUtils.hasText(tenantBusinessId)) {
            throw new SecurityException("Tenant is inactive");
        }
        membershipPolicy.requireTenantPermission(context.tenantRefId(), context.memberRefId(),
                IamPermissionCodes.TENANT_ROLE_MANAGE);
        RoleDO role = roleDao.lockScopedRole(
                context.tenantRefId(), context.appRefId(), context.year(), roleRefId);
        if (role == null || !ROLE_TENANT.equals(role.getRoleType())
                || role.getVersion() == null || role.getVersion() < 0) {
            throw new SecurityException("Role is unavailable or platform-managed");
        }
        List<String> activePermissions = roleDao.selectActivePermissionRefs(
                context.tenantRefId(), roleRefId);
        if (CollectionUtils.isEmpty(activePermissions) || !activePermissions.contains(permissionRefId)) {
            throw new SecurityException("Target role does not contain this action");
        }
        List<RolePermissionGrantVO> permitted = roleDao.selectDelegablePermissions(
                context.tenantRefId(), context.appRefId(), List.of(permissionRefId));
        if (CollectionUtils.isEmpty(permitted) || permitted.size() != 1
                || !permissionRefId.equals(permitted.get(0).getPermissionRefId())
                || !StringUtils.hasText(permitted.get(0).getPermissionCode())
                || !permissionService.hasTenantPermission(
                        context.tenantRefId(), context.memberRefId(), context.appRefId(),
                        context.year(), permitted.get(0).getPermissionCode())) {
            throw new SecurityException("Action is not delegated to the administrator");
        }
        String owner = permitted.get(0).getOwnerType();
        if (!OWNER_TENANT.equals(owner) && !OWNER_SYSTEM.equals(owner)) {
            throw new SecurityException("Action owner is not trusted");
        }
        if (OWNER_SYSTEM.equals(owner) && !hasCompleteSystemActionGrant(context,
                permitted.get(0).getPermissionCode())) {
            throw new SecurityException("System action requires full-scope delegation evidence");
        }
        return new Guard(context, tenantBusinessId, role);
    }

    private boolean hasCompleteSystemActionGrant(TenantSessionContext context, String permissionCode) {
        List<RoleDataGrantVO> grants = authorizationDao.selectDataGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                context.year(), permissionCode);
        if (CollectionUtils.isEmpty(grants)) {
            return false;
        }
        for (RoleDataGrantVO grant : grants) {
            if (grant != null && "TENANT".equals(grant.getBindingScopeType())
                    && !StringUtils.hasText(grant.getBindingOrgRefId())
                    && "TENANT_ALL".equals(grant.getDataScope())
                    && !StringUtils.hasText(grant.getDataOrgRefId())) {
                return true;
            }
        }
        return false;
    }

    private Set<DataRule> validateDataRules(Guard guard, List<SaasDataPolicyUpdateDTO.Rule> proposed) {
        Set<DataRule> distinct = new HashSet<>();
        for (SaasDataPolicyUpdateDTO.Rule input : proposed) {
            if (input == null) {
                throw new IllegalArgumentException("Null data policy is forbidden");
            }
            String scope = input.getDataScope();
            String org = input.getOrgRefId();
            if ("SELF".equals(scope) || "TENANT_ALL".equals(scope)) {
                if (StringUtils.hasText(org)) {
                    throw new IllegalArgumentException("Scope cannot use an organization anchor");
                }
            } else if ("ORG".equals(scope) || "ORG_TREE".equals(scope)) {
                if (!StringUtils.hasText(org)) {
                    throw new IllegalArgumentException("Organization anchor is required");
                }
                OrganizationDO node = hierarchyDao.selectInTenant(org, guard.tenantBusinessId);
                if (node == null || !"ENABLE".equals(node.getStatus())
                        || !Integer.valueOf(0).equals(node.getIsDelete())) {
                    throw new SecurityException("Organization anchor is not active in tenant");
                }
            } else {
                throw new IllegalArgumentException("Unsupported row-level data scope");
            }
            if (!distinct.add(new DataRule(scope, org))) {
                throw new IllegalArgumentException("Duplicate row-level data policy");
            }
        }
        return distinct;
    }

    private Set<FieldRule> validateFieldRules(Guard guard, String resourceCode,
                                               List<SaasFieldPolicyUpdateDTO.Rule> proposed) {
        Set<FieldRule> distinct = new HashSet<>();
        Set<String> fields = new HashSet<>();
        for (SaasFieldPolicyUpdateDTO.Rule input : proposed) {
            if (input == null || !StringUtils.hasText(input.getFieldRefId())
                    || !("ALLOW".equals(input.getReadEffect()) || "MASK".equals(input.getReadEffect())
                         || "DENY".equals(input.getReadEffect()))
                    || !("ALLOW".equals(input.getWriteEffect()) || "DENY".equals(input.getWriteEffect()))
                    || !fields.add(input.getFieldRefId())) {
                throw new IllegalArgumentException("Field policy contains invalid or duplicate entries");
            }
            distinct.add(new FieldRule(input.getFieldRefId(), input.getReadEffect(), input.getWriteEffect()));
        }
        if (!fields.isEmpty()) {
            List<String> allowed = policyDao.selectDelegableFieldRefs(
                    guard.context.tenantRefId(), guard.context.appRefId(),
                    resourceCode, new ArrayList<>(fields));
            if (CollectionUtils.isEmpty(allowed) || allowed.size() != fields.size()
                    || !new HashSet<>(allowed).equals(fields)) {
                throw new SecurityException("Field directory contains unavailable or restricted entries");
            }
        }
        return distinct;
    }

    private void invalidateAndAudit(Guard guard, String roleRefId, String operation) {
        long oldVersion = guard.role.getVersion();
        if (oldVersion == Long.MAX_VALUE || roleDao.incrementRoleVersion(
                guard.context.tenantRefId(), guard.context.appRefId(), guard.context.year(),
                roleRefId, oldVersion, guard.context.memberRefId()) != 1) {
            throw new IllegalStateException("Role policy version conflict");
        }
        if (versionDao.incrementForTenant(guard.context.tenantRefId()) < 1) {
            throw new IllegalStateException("Authorization invalidation failed");
        }
        if (auditDao.append(UniqueIdFacade.nextId(), guard.context.tenantRefId(),
                guard.context.appRefId(), guard.context.year(), guard.context.memberRefId(),
                roleRefId, operation, oldVersion, oldVersion + 1L) != 1) {
            throw new IllegalStateException("Authorization policy audit append failed");
        }
    }

    private static void requireCount(List<?> rules, int limit) {
        if (rules == null || rules.size() > limit) {
            throw new IllegalArgumentException("Invalid policy list size");
        }
    }

    private record DataRule(String scope, String organization) {
    }

    private record FieldRule(String fieldRefId, String readEffect, String writeEffect) {
    }

    private record Guard(TenantSessionContext context, String tenantBusinessId, RoleDO role) {
    }
}
