package com.peach.auth.service;

import com.peach.auth.dao.SaasPolicyCatalogDao;
import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.dao.ScopedDataPolicyManagementDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.RoleDataPolicyVO;
import com.peach.auth.vo.RoleFieldPolicyVO;
import com.peach.auth.vo.RolePermissionGrantVO;
import com.peach.auth.vo.SaasCatalogEntryVO;
import com.peach.auth.vo.SaasCatalogPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 可委派字段目录与当前角色动作的行级/字段策略受信读取。
 *
 * <p>必须先核验真实 Token、当前租户的完整管理权限、角色/动作当前授予及
 * 管理员对该动作的完整授权。SYSTEM 动作另要求同一条 TENANT + TENANT_ALL
 * 授权证据，不能跨不同角色绑定拼接。策略读取不接受客户端提供身份或拼接 SQL。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:55
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaasPolicyCatalogService {

    private static final int MAX_PAGE = 1000;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_DATA = 32;
    private static final int MAX_FIELDS = 128;
    private static final String ROLE_MANAGE = "TENANT.ROLE.MANAGE";
    private static final Pattern PHYSICAL_ID = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final Pattern RESOURCE_CODE = Pattern.compile("^[A-Z][A-Z0-9_.-]{0,63}$");

    private final TenantSessionService sessionService;
    private final ScopedPermissionService permissionService;
    private final ScopedDataAuthorizationDao authorizationDao;
    private final ScopedDataPolicyManagementDao policyDao;
    private final SaasPolicyCatalogDao catalogDao;

    public SaasCatalogPageVO fields(String resourceCode, int page, int pageSize) {
        TenantSessionContext context = requireAdmin();
        requireResourceCode(resourceCode);
        if (page < 1 || page > MAX_PAGE || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid bounded field catalog pagination");
        }
        List<SaasCatalogEntryVO> rows = catalogDao.selectDelegableFields(
                context.tenantRefId(), context.appRefId(), resourceCode,
                pageSize + 1, (page - 1) * pageSize);
        if (rows == null) {
            throw new SecurityException("Delegable field catalog is unavailable");
        }
        if (rows.isEmpty()) {
            return new SaasCatalogPageVO(page, pageSize, false, List.of());
        }
        if (rows.size() > pageSize + 1) {
            throw new SecurityException("Field catalog exceeds requested page boundary");
        }
        return new SaasCatalogPageVO(page, pageSize, rows.size() > pageSize,
                rows.subList(0, Math.min(rows.size(), pageSize)));
    }

    public List<RoleDataPolicyVO> dataPolicies(String roleRefId, String permissionRefId) {
        CheckedAction checked = requireAction(roleRefId, permissionRefId);
        TenantSessionContext context = checked.context();
        List<RoleDataPolicyVO> rows = policyDao.selectDataPolicies(
                context.tenantRefId(), context.appRefId(), roleRefId, permissionRefId);
        if (rows == null) {
            throw new SecurityException("Policy directory is unavailable");
        }
        if (rows.size() > MAX_DATA || rows.stream().anyMatch(row -> row == null)) {
            throw new SecurityException("Incomplete data policy snapshot");
        }
        return List.copyOf(rows);
    }

    public List<RoleFieldPolicyVO> fieldPolicies(String roleRefId, String permissionRefId,
                                                 String resourceCode) {
        CheckedAction checked = requireAction(roleRefId, permissionRefId);
        TenantSessionContext context = checked.context();
        requireResourceCode(resourceCode);
        // 字段资源必须与当前角色真正持有的动作语义一致，不能借无关动作跨资源编辑。
        if (!checked.permissionCode().startsWith(resourceCode + ".")) {
            throw new SecurityException("Field resource does not match the delegated action");
        }
        List<RoleFieldPolicyVO> rows = policyDao.selectFieldPolicies(
                context.tenantRefId(), context.appRefId(), roleRefId, permissionRefId);
        if (rows == null) {
            throw new SecurityException("Policy directory is unavailable");
        }
        if (rows.size() > MAX_FIELDS || rows.stream().anyMatch(row -> row == null)) {
            throw new SecurityException("Incomplete field policy snapshot");
        }
        Set<String> fields = new HashSet<>();
        for (RoleFieldPolicyVO row : rows) {
            if (!StringUtils.hasText(row.getFieldRefId()) || !fields.add(row.getFieldRefId())) {
                throw new SecurityException("Invalid or duplicate field policy");
            }
        }
        if (!fields.isEmpty()) {
            List<String> allowed = policyDao.selectDelegableFieldRefs(
                    context.tenantRefId(), context.appRefId(), resourceCode,
                    new ArrayList<>(fields));
            if (CollectionUtils.isEmpty(allowed) || allowed.size() != fields.size()
                    || !new HashSet<>(allowed).equals(fields)) {
                throw new SecurityException("Stored field policies exceed trusted field directory");
            }
        }
        return List.copyOf(rows);
    }

    private CheckedAction requireAction(String roleRefId, String permissionRefId) {
        TenantSessionContext context = requireAdmin();
        requirePhysicalId(roleRefId);
        requirePhysicalId(permissionRefId);
        RolePermissionGrantVO action = catalogDao.selectScopedRoleAction(
                context.tenantRefId(), context.appRefId(), roleRefId, permissionRefId);
        if (action == null || !permissionRefId.equals(action.getPermissionRefId())
                || !StringUtils.hasText(action.getPermissionCode())
                || !permissionService.hasTenantPermission(context.tenantRefId(),
                context.memberRefId(), context.appRefId(), action.getPermissionCode())) {
            throw new SecurityException("Current role action is not delegable to this administrator");
        }
        if ("SYSTEM".equals(action.getOwnerType()) && !hasCompleteSystemGrant(context,
                action.getPermissionCode())) {
            throw new SecurityException("System action requires complete row-level delegation");
        }
        if (!"SYSTEM".equals(action.getOwnerType()) && !"TENANT".equals(action.getOwnerType())) {
            throw new SecurityException("Untrusted action owner");
        }
        return new CheckedAction(context, action.getPermissionCode());
    }

    private boolean hasCompleteSystemGrant(TenantSessionContext context, String code) {
        List<RoleDataGrantVO> grants = authorizationDao.selectDataGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                code);
        if (CollectionUtils.isEmpty(grants)) {
            return false;
        }
        for (RoleDataGrantVO row : grants) {
            if (row != null && "TENANT".equals(row.getBindingScopeType())
                    && !StringUtils.hasText(row.getBindingOrgRefId())
                    && "TENANT_ALL".equals(row.getDataScope())
                    && !StringUtils.hasText(row.getDataOrgRefId())) {
                return true;
            }
        }
        return false;
    }

    private TenantSessionContext requireAdmin() {
        TenantSessionContext context = sessionService.requireCurrent();
        if (context == null || !permissionService.hasTenantPermission(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                ROLE_MANAGE)) {
            throw new SecurityException("Tenant-wide role management permission is required");
        }
        return context;
    }

    private record CheckedAction(TenantSessionContext context, String permissionCode) {
    }

    private void requirePhysicalId(String id) {
        if (id == null || !PHYSICAL_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Valid physical role or permission ID required");
        }
    }

    private void requireResourceCode(String resourceCode) {
        if (resourceCode == null || !RESOURCE_CODE.matcher(resourceCode).matches()) {
            throw new IllegalArgumentException("Invalid trusted resource code");
        }
    }
}
