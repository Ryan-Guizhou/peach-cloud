package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.ScopedRoleBindingDao;
import com.peach.auth.entity.MemberRoleBindingDO;
import com.peach.auth.entity.OrganizationDO;
import com.peach.auth.entity.TenantMemberDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.IamPermissionCodes;
import com.peach.common.unique.UniqueIdFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 独立角色绑定的授权、撤销和同步版本失效事务。
 *
 * <p>所有成员授权变更先进行 Token 管理授权校验，再锁定租户及目标成员，
 * 在同一事务中写入绑定、递增 AUTH_VERSION、追加审计；任一步失败整体回滚。
 * 角色权限只能在操作者自己拥有完整租户权限且权限可委派时授出。
 * 本服务不开放未认证 REST 入口，服务调用者不得自行声明管理身份。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScopedRoleBindingService {

    private static final String SCOPE_TENANT = "TENANT";
    private static final String SCOPE_ORG = "ORG";
    private static final String SCOPE_ORG_TREE = "ORG_TREE";

    private final TenantMembershipPolicy membershipPolicy;
    private final TenantSessionService sessionAdapter;
    private final ScopedPermissionService permissionService;
    private final ScopedRoleBindingDao bindingDao;
    private final OrganizationHierarchyDao hierarchyDao;
    private final MemberAuthVersionDao memberAuthVersionDao;
    private final IamAuthorizationAuditDao auditDao;

    /**
     * 将指定角色授予一个成员的独立应用/年度/组织范围；相同有效绑定幂等返回。
     *
     * @param tenantRefId 租户物理 ID
     * @param operatorMemberRefId 操作人成员物理 ID，必须与 Token 一致
     * @param targetMemberRefId 目标成员物理 ID
     * @param roleRefId 角色物理 ID
     * @param scopeType TENANT、ORG 或 ORG_TREE
     * @param scopeRefId ORG/ORG_TREE 的机构物理 ID，TENANT 必须为空
     * @return 授权绑定物理 ID
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public String grant(String tenantRefId, String operatorMemberRefId, String targetMemberRefId,
                        String roleRefId, String scopeType, String scopeRefId) {
        TenantSessionContext current = requireAdminScope(tenantRefId, operatorMemberRefId);
        validateTarget(targetMemberRefId, roleRefId);
        if (operatorMemberRefId.equals(targetMemberRefId)) {
            throw new SecurityException("Administrators cannot grant roles to themselves");
        }
        String tenantBusinessId = lockTenantAndReauthorize(tenantRefId, operatorMemberRefId);
        validateScope(scopeType, scopeRefId, tenantBusinessId);
        TenantMemberDO target = lockMember(tenantRefId, targetMemberRefId);
        if (bindingDao.countActiveRole(tenantRefId, current.appRefId(),
                current.year(), roleRefId) != 1) {
            throw new SecurityException("Role is not active in the selected tenant, app and year");
        }
        assertRoleDelegationAllowed(current, tenantRefId, roleRefId);

        MemberRoleBindingDO existing = bindingDao.selectActiveBinding(
                tenantRefId, targetMemberRefId, current.appRefId(), current.year(),
                roleRefId, scopeType, scopeRefId);
        if (existing != null) {
            return existing.getId();
        }

        MemberRoleBindingDO binding = new MemberRoleBindingDO();
        binding.setId(UniqueIdFacade.nextId());
        binding.setTenantRefId(tenantRefId);
        binding.setMemberRefId(targetMemberRefId);
        binding.setAppRefId(current.appRefId());
        binding.setRoleRefId(roleRefId);
        binding.setAuthYear(current.year());
        binding.setScopeType(scopeType);
        binding.setScopeRefId(scopeRefId);
        binding.setStatus("ACTIVE");
        binding.fillCreateTime(operatorMemberRefId);
        if (bindingDao.insert(binding) != 1) {
            throw new IllegalStateException("Role binding creation failed");
        }
        incrementVersionAndAudit(target, current, operatorMemberRefId, binding.getId(), "ROLE_GRANT");
        log.info("Scoped role granted, tenantRefId={}, bindingRefId={}", tenantRefId, binding.getId());
        return binding.getId();
    }

    /**
     * 撤销一条绑定并保留历史记录；撤销立即递增目标成员授权版本。
     *
     * @param tenantRefId 租户物理 ID
     * @param operatorMemberRefId 当前操作人成员物理 ID
     * @param bindingRefId 被撤销授权的物理 ID
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void revoke(String tenantRefId, String operatorMemberRefId, String bindingRefId) {
        TenantSessionContext current = requireAdminScope(tenantRefId, operatorMemberRefId);
        requireId(bindingRefId, "bindingRefId");
        lockTenantAndReauthorize(tenantRefId, operatorMemberRefId);
        MemberRoleBindingDO binding = bindingDao.selectActiveById(tenantRefId, bindingRefId);
        if (binding == null || !current.appRefId().equals(binding.getAppRefId())
                || binding.getAuthYear() == null || current.year() != binding.getAuthYear()) {
            throw new SecurityException("Role binding is outside current session scope");
        }
        if (operatorMemberRefId.equals(binding.getMemberRefId())) {
            throw new SecurityException("Administrators cannot revoke their own role bindings");
        }
        // 撤销是减少权限，不可要求角色的权限当前仍可委派，否则权限收回会被意外阻断。
        TenantMemberDO target = lockMember(tenantRefId, binding.getMemberRefId());
        if (binding.getVersion() == null || binding.getVersion() < 0
                || bindingDao.revoke(tenantRefId, bindingRefId, binding.getVersion()) != 1) {
            throw new IllegalStateException("Role binding revoke conflicted");
        }
        incrementVersionAndAudit(target, current, operatorMemberRefId, bindingRefId, "ROLE_REVOKE");
        log.info("Scoped role revoked, tenantRefId={}, bindingRefId={}", tenantRefId, bindingRefId);
    }

    private TenantSessionContext requireAdminScope(String tenantRefId, String operatorMemberRefId) {
        requireId(tenantRefId, "tenantRefId");
        requireId(operatorMemberRefId, "operatorMemberRefId");
        membershipPolicy.requireTenantPermission(
                tenantRefId, operatorMemberRefId, IamPermissionCodes.TENANT_ROLE_MANAGE);
        TenantSessionContext current = sessionAdapter.requireCurrent();
        if (!tenantRefId.equals(current.tenantRefId())
                || !operatorMemberRefId.equals(current.memberRefId())) {
            throw new SecurityException("Role administration token scope mismatch");
        }
        return current;
    }

    private String lockTenantAndReauthorize(String tenantRefId, String operatorMemberRefId) {
        String businessId = hierarchyDao.lockTenantAndFindBusinessId(tenantRefId);
        if (!StringUtils.hasText(businessId)) {
            throw new SecurityException("Tenant is inactive");
        }
        // 与同租户的角色管理写事务串行，避免管理员授权在锁等待时被撤销。
        membershipPolicy.requireTenantPermission(
                tenantRefId, operatorMemberRefId, IamPermissionCodes.TENANT_ROLE_MANAGE);
        return businessId;
    }

    private TenantMemberDO lockMember(String tenantRefId, String memberRefId) {
        TenantMemberDO target = bindingDao.lockActiveMember(tenantRefId, memberRefId);
        if (target == null || target.getAuthVersion() == null || target.getAuthVersion() < 0) {
            throw new SecurityException("Target member is not active or has invalid authorization version");
        }
        return target;
    }

    private void validateScope(String scopeType, String scopeRefId, String tenantBusinessId) {
        if (SCOPE_TENANT.equals(scopeType)) {
            if (StringUtils.hasText(scopeRefId)) {
                throw new IllegalArgumentException("Tenant scope must not specify an organization");
            }
            return;
        }
        if (!SCOPE_ORG.equals(scopeType) && !SCOPE_ORG_TREE.equals(scopeType)) {
            throw new IllegalArgumentException("Unsupported role binding scope");
        }
        requireId(scopeRefId, "scopeRefId");
        OrganizationDO org = hierarchyDao.selectInTenant(scopeRefId, tenantBusinessId);
        if (org == null || !Integer.valueOf(0).equals(org.getIsDelete())
                || !"ENABLE".equals(org.getStatus())) {
            throw new SecurityException("Organization scope is inactive or belongs to another tenant");
        }
    }

    private void assertRoleDelegationAllowed(TenantSessionContext current,
                                             String tenantRefId, String roleRefId) {
        int assigned = bindingDao.countActiveRolePermissions(tenantRefId, roleRefId);
        List<String> codes = bindingDao.selectDelegablePermissionCodes(
                tenantRefId, current.appRefId(), roleRefId);
        if (assigned < 1 || CollectionUtils.isEmpty(codes) || assigned != codes.size()) {
            throw new SecurityException("Role includes an unavailable or non-delegable permission");
        }
        Set<String> distinct = new HashSet<>(codes);
        if (distinct.size() != assigned) {
            throw new SecurityException("Duplicate permission semantic code in role");
        }
        for (String code : distinct) {
            if (!permissionService.hasTenantPermission(
                    tenantRefId, current.memberRefId(), current.appRefId(), current.year(), code)) {
                throw new SecurityException("Administrator cannot delegate a permission they do not hold");
            }
        }
    }

    private void incrementVersionAndAudit(TenantMemberDO target, TenantSessionContext current,
                                          String operatorRefId, String bindingRefId, String operation) {
        long original = target.getAuthVersion();
        if (original == Long.MAX_VALUE) {
            throw new IllegalStateException("Authorization version has exhausted");
        }
        if (memberAuthVersionDao.incrementForMember(current.tenantRefId(), target.getId()) != 1) {
            throw new IllegalStateException("Authorization version update failed");
        }
        if (auditDao.append(UniqueIdFacade.nextId(), current.tenantRefId(), current.appRefId(),
                current.year(), operatorRefId, bindingRefId, operation, original, original + 1L) != 1) {
            throw new IllegalStateException("Authorization audit append failed");
        }
    }

    private static void validateTarget(String memberRefId, String roleRefId) {
        requireId(memberRefId, "memberRefId");
        requireId(roleRefId, "roleRefId");
    }

    private static void requireId(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
