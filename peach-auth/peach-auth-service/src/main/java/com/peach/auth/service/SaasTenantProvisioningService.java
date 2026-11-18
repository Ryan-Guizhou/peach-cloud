package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.ScopedRoleBindingDao;
import com.peach.auth.dao.ScopedRoleManagementDao;
import com.peach.auth.dao.TenantMemberDao;
import com.peach.auth.dao.TenantProvisioningDao;
import com.peach.auth.dto.SaasTenantCreateDTO;
import com.peach.auth.entity.MemberRoleBindingDO;
import com.peach.auth.entity.RoleDO;
import com.peach.auth.entity.TenantMemberDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;
import com.peach.auth.vo.RolePermissionGrantVO;
import com.peach.auth.vo.SaasTenantCreatedVO;
import com.peach.common.PeachDO;
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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 由系统保护的平台管理员在单个事务中开通租户及其初始所有者。
 *
 * <p>首先验证服务端 Token，再锁定真正的 PLATFORM 控制租户并复核非委派 SYSTEM 权限。
 * 创建租户、应用、根机构、所有者成员、角色、权限绑定及审计全部在事务中成功才提交。
 * 不能通过前端角色名、TENANT_CODE 或请求体中的操作者信息升级为平台身份。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 18:40
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaasTenantProvisioningService {

    private static final Pattern TENANT_CODE = Pattern.compile("[A-Z][A-Z0-9_-]{2,63}");
    private static final Pattern PHYSICAL_ID = Pattern.compile("[a-fA-F0-9]{32}");
    private static final String OWNER_ROLE_CODE = "ROLE_TENANT_OWNER";
    private static final int MAX_OWNER_PERMISSIONS = 256;
    private static final Set<String> RESERVED_CODES =
            Set.of("PLATFORM", "SYSTEM", "DEFAULT", "ROOT");
    private static final Set<String> REQUIRED_OWNER_CODES = Set.of(
            "TENANT.MEMBER.MANAGE",
            "TENANT.ORG.MANAGE",
            "TENANT.ROLE.MANAGE",
            "POST:/api/auth/saas/tenants/members/invitations",
            "DELETE:/api/auth/saas/tenants/members/invitations/{invitationRefId}",
            "POST:/api/auth/saas/tenants/members/{memberRefId}/disable",
            "POST:/api/auth/saas/tenants/members/{memberRefId}/enable",
            "POST:/api/auth/saas/tenants/organizations",
            "PUT:/api/auth/saas/tenants/organizations/{orgRefId}",
            "DELETE:/api/auth/saas/tenants/organizations/{orgRefId}",
            "POST:/api/auth/saas/tenants/organizations/{orgRefId}/move");

    private final TenantSessionService tenantSessionService;
    private final TenantProvisioningDao provisioningDao;
    private final TenantMemberDao tenantMemberDao;
    private final ScopedRoleManagementDao roleManagementDao;
    private final ScopedRoleBindingDao bindingDao;
    private final MemberAuthVersionDao authVersionDao;
    private final IamAuthorizationAuditDao auditDao;
    private final AuthBusinessIdGenerator businessIdGenerator;

    /**
     * 创建一个可直接由初始所有者管理的独立 SaaS 租户。
     *
     * @param request 业务名称、编码和目标已验证全局用户物理 ID
     * @return 新租户、应用、根机构及所有者身份
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public SaasTenantCreatedVO create(SaasTenantCreateDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Tenant provisioning request is required");
        }
        String code = requireTenantCode(request.getTenantCode());
        String name = requireName(request.getTenantName(), 128, "tenantName");
        String appName = requireName(request.getAppName(), 64, "appName");
        String ownerUserRefId = request.getOwnerUserRefId();
        if (ownerUserRefId == null || !PHYSICAL_ID.matcher(ownerUserRefId).matches()) {
            throw new IllegalArgumentException("Valid owner user physical ID is required");
        }

        TenantSessionContext operator = tenantSessionService.requireCurrent();
        if (!operator.tenantRefId().equals(provisioningDao.lockPlatformTenant(operator.tenantRefId()))
                || provisioningDao.countPlatformCreateGrant(
                operator.tenantRefId(), operator.memberRefId(), operator.appRefId()) != 1) {
            throw new SecurityException("Trusted platform tenant creation privilege is required");
        }
        if (tenantMemberDao.countEnabledUser(ownerUserRefId) != 1) {
            throw new SecurityException("Initial tenant owner is not an active global account");
        }

        List<RolePermissionGrantVO> permissions = provisioningDao.selectOwnerPermissions();
        Set<String> availableCodes = new HashSet<>();
        if (CollectionUtils.isEmpty(permissions) || permissions.size() > MAX_OWNER_PERMISSIONS) {
            throw new IllegalStateException("Initial system authorization catalog is unavailable");
        }
        for (RolePermissionGrantVO permission : permissions) {
            if (permission == null || !StringUtils.hasText(permission.getPermissionRefId())
                    || !StringUtils.hasText(permission.getPermissionCode())
                    || !availableCodes.add(permission.getPermissionCode())) {
                throw new IllegalStateException("Initial system authorization catalog is inconsistent");
            }
        }
        if (!availableCodes.containsAll(REQUIRED_OWNER_CODES)) {
            throw new IllegalStateException("Required system tenant management permissions are missing");
        }

        String tenantRefId = UniqueIdFacade.nextId();
        String tenantId = businessIdGenerator.next(AuthBusinessIdType.TENANT);
        String appRefId = UniqueIdFacade.nextId();
        String appId = businessIdGenerator.next(AuthBusinessIdType.APP);
        String orgRefId = UniqueIdFacade.nextId();
        String orgId = businessIdGenerator.next(AuthBusinessIdType.ORG);
        String now = PeachDO.getCurrentTime();
        String actorRefId = operator.memberRefId();

        verifyInsert(provisioningDao.insertTenant(
                tenantRefId, tenantId, code, name, actorRefId, now), "tenant");
        verifyInsert(provisioningDao.insertApplication(
                appRefId, appId, tenantId, appName, actorRefId, now), "application");
        verifyInsert(provisioningDao.insertRootOrganization(
                orgRefId, orgId, tenantId, "默认机构", actorRefId, now), "root organization");

        TenantMemberDO owner = new TenantMemberDO();
        owner.setId(UniqueIdFacade.nextId());
        owner.setMemberId(businessIdGenerator.next(AuthBusinessIdType.MEMBER));
        owner.setTenantRefId(tenantRefId);
        owner.setUserRefId(ownerUserRefId);
        owner.setStatus("ACTIVE");
        owner.setVersion(0L);
        owner.setAuthVersion(0L);
        owner.fillCreateTime(actorRefId);
        verifyInsert(tenantMemberDao.insert(owner), "owner membership");

        RoleDO role = new RoleDO();
        role.setId(UniqueIdFacade.nextId());
        role.setRoleId(businessIdGenerator.next(AuthBusinessIdType.ROLE));
        role.setTenantId(tenantId);
        role.setOrgId("TENANT");
        role.setRoleCode(OWNER_ROLE_CODE);
        role.setRoleName("租户所有者");
        role.setRoleScope("TENANT");
        role.setRoleType("SYSTEM");
        role.setTenantRefId(tenantRefId);
        role.setAppRefId(appRefId);
        role.setRoleStatus("ACTIVE");
        role.setVersion(0L);
        role.fillCreateTime(actorRefId);
        verifyInsert(provisioningDao.insertOwnerRole(role), "protected owner role");

        for (RolePermissionGrantVO permission : permissions) {
            verifyInsert(roleManagementDao.insertRolePermission(
                    UniqueIdFacade.nextId(), tenantRefId, role.getId(),
                    permission.getPermissionRefId(), actorRefId), "owner permission");
        }

        MemberRoleBindingDO binding = new MemberRoleBindingDO();
        binding.setId(UniqueIdFacade.nextId());
        binding.setTenantRefId(tenantRefId);
        binding.setMemberRefId(owner.getId());
        binding.setAppRefId(appRefId);
        binding.setRoleRefId(role.getId());
        binding.setScopeType("TENANT");
        binding.setStatus("ACTIVE");
        binding.fillCreateTime(actorRefId);
        verifyInsert(bindingDao.insert(binding), "owner role binding");
        verifyInsert(authVersionDao.incrementForMember(tenantRefId, owner.getId()),
                "owner authorization version");
        verifyInsert(auditDao.append(UniqueIdFacade.nextId(), tenantRefId, appRefId,
                actorRefId, tenantRefId, "TENANT_CREATE", 0L, 0L), "tenant creation audit");
        verifyInsert(auditDao.append(UniqueIdFacade.nextId(), tenantRefId, appRefId,
                actorRefId, binding.getId(), "TENANT_OWNER_GRANT", 0L, 1L), "owner authorization audit");

        log.info("SaaS tenant provisioned, tenantRefId={}, ownerMemberRefId={}",
                tenantRefId, owner.getId());
        return new SaasTenantCreatedVO(tenantRefId, tenantId, appRefId, appId,
                orgRefId, orgId, owner.getId());
    }

    private static String requireTenantCode(String input) {
        if (!StringUtils.hasText(input)) {
            throw new IllegalArgumentException("Tenant code is required");
        }
        String code = input.trim().toUpperCase(Locale.ENGLISH);
        if (!TENANT_CODE.matcher(code).matches() || RESERVED_CODES.contains(code)) {
            throw new IllegalArgumentException("Tenant code is invalid or reserved");
        }
        return code;
    }

    private static String requireName(String input, int maxLength, String field) {
        if (!StringUtils.hasText(input) || input.trim().length() > maxLength) {
            throw new IllegalArgumentException(field + " is invalid");
        }
        return input.trim();
    }

    private static void verifyInsert(int affected, String target) {
        if (affected != 1) {
            throw new IllegalStateException("Failed to persist tenant " + target);
        }
    }
}
