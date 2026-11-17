package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.ScopedRoleManagementDao;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;
import com.peach.auth.service.support.IamPermissionCodes;
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
 * SaaS 租户角色创建与角色权限差量同步的安全业务入口。
 *
 * <p>新角色属于物理租户、物理应用和单一自然年度。
 * 权限变更使用租户锁及角色锁串行化，必须与成员授权版本失效、
 * 操作审计同事务提交；禁止租户管理员修改平台 SYSTEM 角色，
 * 也不能授予自身未持有或标记为不可委派的权限。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScopedRoleManagementService {

    private static final Pattern ROLE_CODE_PATTERN =
            Pattern.compile("ROLE_[A-Z0-9_]{1,15}");
    private static final int MAX_PERMISSION_ASSIGNMENTS = 200;
    private static final String TENANT_ROLE_TYPE = "TENANT";

    private final TenantMembershipPolicy membershipPolicy;
    private final TenantSessionService sessionService;
    private final ScopedRoleManagementDao roleManagementDao;
    private final OrganizationHierarchyDao hierarchyDao;
    private final ScopedPermissionService permissionService;
    private final MemberAuthVersionDao versionDao;
    private final IamAuthorizationAuditDao auditDao;
    private final AuthBusinessIdGenerator businessIdGenerator;

    /**
     * 创建一条只属于当前应用、年度和租户的新角色。
     *
     * @param tenantRefId 租户物理主键
     * @param operatorMemberRefId 已认证的管理员成员物理 ID
     * @param roleCode 稳定角色代码，必须使用 ROLE_ 前缀
     * @param roleName 角色名称
     * @return 新角色物理主键
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public String createRole(String tenantRefId, String operatorMemberRefId,
                             String roleCode, String roleName) {
        TenantSessionContext context = requireAdminScope(tenantRefId, operatorMemberRefId);
        if (roleCode == null || !ROLE_CODE_PATTERN.matcher(roleCode).matches()
                || !StringUtils.hasText(roleName) || roleName.length() > 64) {
            throw new IllegalArgumentException("Invalid role code or name");
        }
        String tenantBusinessId = lockTenantAndReauthorize(tenantRefId, operatorMemberRefId);
        RoleDO role = new RoleDO();
        role.setId(UniqueIdFacade.nextId());
        role.setRoleId(businessIdGenerator.next(AuthBusinessIdType.ROLE));
        role.setTenantId(tenantBusinessId);
        role.setOrgId("TENANT");
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setRoleType(TENANT_ROLE_TYPE);
        role.setTenantRefId(tenantRefId);
        role.setAppRefId(context.appRefId());
        role.setAuthYear(context.year());
        role.setRoleStatus("ACTIVE");
        role.setVersion(0L);
        role.fillCreateTime(operatorMemberRefId);
        if (roleManagementDao.insertRole(role) != 1) {
            throw new IllegalStateException("Scoped role creation failed");
        }
        appendAudit(context, operatorMemberRefId, role.getId(), "ROLE_CREATE", 0L, 0L);
        log.info("Scoped tenant role created, roleRefId={}, tenantRefId={}", role.getId(), tenantRefId);
        return role.getId();
    }

    /**
     * 对某个租户角色按差集同步完整权限集合。
     *
     * <p>仅当前租户创建的 TENANT 类型角色允许编辑；平台 SYSTEM 角色不可修改。
     * 所有新增权限都必须处于当前管理员具有完整租户授权且可委派的集合中。</p>
     *
     * @param tenantRefId 租户物理主键
     * @param operatorMemberRefId 当前操作者成员物理主键
     * @param roleRefId 当前租户的角色物理主键
     * @param requestedPermissionRefIds 期望的完整权限物理 ID 列表，空列表表示全部撤销
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void replacePermissions(String tenantRefId, String operatorMemberRefId,
                                   String roleRefId, List<String> requestedPermissionRefIds) {
        TenantSessionContext context = requireAdminScope(tenantRefId, operatorMemberRefId);
        requireId(roleRefId, "roleRefId");
        lockTenantAndReauthorize(tenantRefId, operatorMemberRefId);
        RoleDO role = roleManagementDao.lockScopedRole(
                tenantRefId, context.appRefId(), context.year(), roleRefId);
        if (role == null || !TENANT_ROLE_TYPE.equals(role.getRoleType())
                || role.getVersion() == null || role.getVersion() < 0) {
            throw new SecurityException("Role is inactive, outside scope, or platform-protected");
        }
        Set<String> requested = validatePermissionIds(requestedPermissionRefIds);
        Set<String> previous = new HashSet<>();
        List<String> stored = roleManagementDao.selectActivePermissionRefs(tenantRefId, roleRefId);
        if (!CollectionUtils.isEmpty(stored)) {
            previous.addAll(stored);
        }
        if (previous.equals(requested)) {
            return;
        }
        assertMayDelegate(context, requested);

        for (String permissionRefId : previous) {
            if (!requested.contains(permissionRefId)
                    && roleManagementDao.revokeRolePermission(tenantRefId, roleRefId, permissionRefId) != 1) {
                throw new IllegalStateException("Role permission revocation conflicted");
            }
        }
        for (String permissionRefId : requested) {
            if (!previous.contains(permissionRefId)
                    && roleManagementDao.insertRolePermission(UniqueIdFacade.nextId(), tenantRefId,
                    roleRefId, permissionRefId, operatorMemberRefId) != 1) {
                throw new IllegalStateException("Role permission grant conflicted");
            }
        }
        long oldVersion = role.getVersion();
        if (oldVersion == Long.MAX_VALUE || roleManagementDao.incrementRoleVersion(
                tenantRefId, context.appRefId(), context.year(), roleRefId,
                oldVersion, operatorMemberRefId) != 1) {
            throw new IllegalStateException("Role authorization version conflict");
        }
        // 角色被多个年度/应用成员引用时，保守撤销租户内所有旧授权快照。
        if (versionDao.incrementForTenant(tenantRefId) < 1) {
            throw new IllegalStateException("Tenant member authorization invalidation failed");
        }
        appendAudit(context, operatorMemberRefId, roleRefId,
                "ROLE_PERMISSION_SYNC", oldVersion, oldVersion + 1L);
        log.info("Scoped role permissions changed, roleRefId={}, tenantRefId={}",
                roleRefId, tenantRefId);
    }

    private void assertMayDelegate(TenantSessionContext context, Set<String> requested) {
        if (requested.isEmpty()) {
            return;
        }
        List<RolePermissionGrantVO> permitted = roleManagementDao.selectDelegablePermissions(
                context.tenantRefId(), context.appRefId(), new ArrayList<>(requested));
        if (CollectionUtils.isEmpty(permitted) || permitted.size() != requested.size()) {
            throw new SecurityException("Role contains a non-delegable or foreign permission");
        }
        Set<String> encountered = new HashSet<>();
        for (RolePermissionGrantVO permission : permitted) {
            if (permission == null || !requested.contains(permission.getPermissionRefId())
                    || !encountered.add(permission.getPermissionRefId())
                    || !StringUtils.hasText(permission.getPermissionCode())
                    || !permissionService.hasTenantPermission(
                    context.tenantRefId(), context.memberRefId(), context.appRefId(),
                    context.year(), permission.getPermissionCode())) {
                throw new SecurityException("Administrator cannot delegate the requested permission");
            }
        }
    }

    private Set<String> validatePermissionIds(List<String> ids) {
        if (ids == null || ids.size() > MAX_PERMISSION_ASSIGNMENTS) {
            throw new IllegalArgumentException("Invalid role permission count");
        }
        Set<String> distinct = new HashSet<>();
        for (String id : ids) {
            requireId(id, "permissionRefId");
            if (!distinct.add(id)) {
                throw new IllegalArgumentException("Duplicate role permission ID");
            }
        }
        return distinct;
    }

    private TenantSessionContext requireAdminScope(String tenantRefId, String operatorMemberRefId) {
        requireId(tenantRefId, "tenantRefId");
        requireId(operatorMemberRefId, "operatorMemberRefId");
        membershipPolicy.requireTenantPermission(
                tenantRefId, operatorMemberRefId, IamPermissionCodes.TENANT_ROLE_MANAGE);
        TenantSessionContext current = sessionService.requireCurrent();
        if (!tenantRefId.equals(current.tenantRefId())
                || !operatorMemberRefId.equals(current.memberRefId())) {
            throw new SecurityException("Role administrator token does not match target tenant");
        }
        return current;
    }

    private String lockTenantAndReauthorize(String tenantRefId, String operatorMemberRefId) {
        String tenantBusinessId = hierarchyDao.lockTenantAndFindBusinessId(tenantRefId);
        if (!StringUtils.hasText(tenantBusinessId)) {
            throw new SecurityException("Tenant is not enabled");
        }
        membershipPolicy.requireTenantPermission(
                tenantRefId, operatorMemberRefId, IamPermissionCodes.TENANT_ROLE_MANAGE);
        return tenantBusinessId;
    }

    private void appendAudit(TenantSessionContext context, String operatorRefId,
                             String targetId, String operation, long before, long after) {
        if (auditDao.append(UniqueIdFacade.nextId(), context.tenantRefId(),
                context.appRefId(), context.year(), operatorRefId, targetId,
                operation, before, after) != 1) {
            throw new IllegalStateException("Scoped role audit append failed");
        }
    }

    private static void requireId(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
