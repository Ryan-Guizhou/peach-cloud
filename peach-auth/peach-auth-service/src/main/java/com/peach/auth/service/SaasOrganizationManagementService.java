package com.peach.auth.service;

import com.peach.auth.dao.IamAuthorizationAuditDao;
import com.peach.auth.dao.MemberAuthVersionDao;
import com.peach.auth.dao.OrganizationHierarchyDao;
import com.peach.auth.dao.SaasOrganizationManagementDao;
import com.peach.auth.dto.SaasOrganizationCreateDTO;
import com.peach.auth.entity.OrganizationDO;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.AuthBusinessIdGenerator;
import com.peach.auth.service.support.AuthBusinessIdType;
import com.peach.auth.service.support.IamPermissionCodes;
import com.peach.common.unique.UniqueIdFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * SaaS 当前租户机构的受控创建、重命名及逻辑删除。
 *
 * <p>全部写入先锁定可信物理租户，按实体物理 ID 定位机构并进行版本 CAS。
 * 删除前检查组织树、成员、角色及授权策略依赖；变更和授权版本失效同事务审计。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaasOrganizationManagementService {

    private static final Pattern ORG_CODE = Pattern.compile("[A-Z][A-Z0-9_-]{1,63}");
    private static final String ENABLED = "ENABLE";
    private static final String ROOT_CODE = "DEFAULT";

    private final TenantSessionService sessionService;
    private final TenantMembershipPolicy membershipPolicy;
    private final OrganizationHierarchyDao hierarchyDao;
    private final SaasOrganizationManagementDao managementDao;
    private final MemberAuthVersionDao authVersionDao;
    private final IamAuthorizationAuditDao auditDao;
    private final AuthBusinessIdGenerator businessIdGenerator;

    /**
     * 创建机构，父节点只允许使用同租户的真实物理 ID。
     *
     * @param request 创建参数
     * @return 新机构物理 ID
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public String create(SaasOrganizationCreateDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Organization request is required");
        }
        String code = requireCode(request.getOrgCode());
        String name = requireName(request.getOrgName());
        TenantSessionContext actor = requireAdmin();
        String tenantId = lockAndReauthorize(actor);
        if (StringUtils.hasText(request.getParentOrgRefId())) {
            requireActiveOrganization(request.getParentOrgRefId(), tenantId);
        }

        OrganizationDO organization = new OrganizationDO();
        organization.setId(UniqueIdFacade.nextId());
        organization.setOrgId(businessIdGenerator.next(AuthBusinessIdType.ORG));
        organization.setTenantId(tenantId);
        organization.setParentOrgRefId(request.getParentOrgRefId());
        organization.setOrgCode(code);
        organization.setOrgName(name);
        organization.setStatus(ENABLED);
        organization.setVersion(0L);
        organization.fillCreateTime(actor.memberRefId());
        verifyUpdated(managementDao.insert(organization), "organization creation");
        invalidateAndAudit(actor, organization.getId(), "ORG_CREATE", 0L, 0L);
        log.info("SaaS organization created, tenantRefId={}, orgRefId={}",
                actor.tenantRefId(), organization.getId());
        return organization.getId();
    }

    /**
     * 保留机构编码及业务编号，只修改展示名称。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void rename(String orgRefId, String orgName) {
        requireId(orgRefId);
        String name = requireName(orgName);
        TenantSessionContext actor = requireAdmin();
        String tenantId = lockAndReauthorize(actor);
        OrganizationDO organization = requireActiveOrganization(orgRefId, tenantId);
        if (Objects.equals(name, organization.getOrgName())) {
            return;
        }
        long version = requireVersion(organization);
        verifyUpdated(managementDao.updateName(orgRefId, tenantId, name, version,
                actor.memberRefId()), "organization rename");
        invalidateAndAudit(actor, orgRefId, "ORG_RENAME", version, version + 1L);
    }

    /**
     * 仅当所有业务、授权、下属机构依赖均为零时允许删除非默认根机构。
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void retire(String orgRefId) {
        requireId(orgRefId);
        TenantSessionContext actor = requireAdmin();
        String tenantId = lockAndReauthorize(actor);
        OrganizationDO organization = requireActiveOrganization(orgRefId, tenantId);
        if (ROOT_CODE.equals(organization.getOrgCode())) {
            throw new SecurityException("Default tenant root organization cannot be removed");
        }
        if (managementDao.countDependencies(actor.tenantRefId(), tenantId,
                orgRefId, organization.getOrgId()) != 0) {
            throw new IllegalStateException("Organization has dependent users, roles or authorization scopes");
        }
        long version = requireVersion(organization);
        verifyUpdated(managementDao.retire(orgRefId, tenantId, version, actor.memberRefId()),
                "organization deletion");
        invalidateAndAudit(actor, orgRefId, "ORG_DELETE", version, version + 1L);
    }

    private TenantSessionContext requireAdmin() {
        TenantSessionContext actor = sessionService.requireCurrent();
        membershipPolicy.requireTenantPermission(actor.tenantRefId(), actor.memberRefId(),
                IamPermissionCodes.TENANT_ORG_MANAGE);
        return actor;
    }

    private String lockAndReauthorize(TenantSessionContext actor) {
        String tenantId = hierarchyDao.lockTenantAndFindBusinessId(actor.tenantRefId());
        if (!StringUtils.hasText(tenantId)) {
            throw new SecurityException("Current SaaS tenant is unavailable");
        }
        membershipPolicy.requireTenantPermission(actor.tenantRefId(), actor.memberRefId(),
                IamPermissionCodes.TENANT_ORG_MANAGE);
        return tenantId;
    }

    private OrganizationDO requireActiveOrganization(String id, String tenantId) {
        OrganizationDO org = hierarchyDao.selectInTenant(id, tenantId);
        if (org == null || !ENABLED.equals(org.getStatus())
                || !Integer.valueOf(0).equals(org.getIsDelete())) {
            throw new SecurityException("Organization is inactive or belongs to another tenant");
        }
        return org;
    }

    private void invalidateAndAudit(TenantSessionContext actor, String orgRefId,
                                    String operation, long previous, long next) {
        if (authVersionDao.incrementForTenant(actor.tenantRefId()) < 1) {
            throw new IllegalStateException("Organization authorization invalidation failed");
        }
        if (auditDao.append(UniqueIdFacade.nextId(), actor.tenantRefId(), actor.appRefId(),
                actor.memberRefId(), orgRefId, operation, previous, next) != 1) {
            throw new IllegalStateException("Organization audit append failed");
        }
    }

    private static String requireCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Organization code is required");
        }
        String code = value.trim().toUpperCase(Locale.ENGLISH);
        if (!ORG_CODE.matcher(code).matches() || ROOT_CODE.equals(code)) {
            throw new IllegalArgumentException("Organization code is invalid or reserved");
        }
        return code;
    }

    private static String requireName(String value) {
        if (!StringUtils.hasText(value) || value.trim().length() > 128) {
            throw new IllegalArgumentException("Organization name is invalid");
        }
        return value.trim();
    }

    private static void requireId(String value) {
        if (!StringUtils.hasText(value) || value.length() > 32) {
            throw new IllegalArgumentException("Organization physical ID is required");
        }
    }

    private static long requireVersion(OrganizationDO organization) {
        if (organization.getVersion() == null
                || organization.getVersion() < 0 || organization.getVersion() == Long.MAX_VALUE) {
            throw new IllegalStateException("Organization version is invalid");
        }
        return organization.getVersion();
    }

    private static void verifyUpdated(int rows, String operation) {
        if (rows != 1) {
            throw new IllegalStateException("Concurrent " + operation + " conflict");
        }
    }
}
