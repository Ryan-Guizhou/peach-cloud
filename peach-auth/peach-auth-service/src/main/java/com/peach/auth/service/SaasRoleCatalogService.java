package com.peach.auth.service;

import com.peach.auth.dao.SaasRoleCatalogDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.SaasCatalogEntryVO;
import com.peach.auth.vo.SaasCatalogPageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 当前有效年度成员的可委派角色与最小权限目录查询。
 *
 * <p>权限身份只由当前 Sa-Token Session 经过数据库实时验证获取。
 * 所有目录查询必须具备当前租户独立 TENANT 范围的角色管理权限，
 * 并且 SELECT 查询已限定物理租户、应用和自然年度。
 * 不返回用户敏感字段，不接受客户端自报身份 ID。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:30
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SaasRoleCatalogService {

    private static final String MANAGE_PERMISSION = "TENANT.ROLE.MANAGE";
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_PAGE = 1000;
    private static final int MAX_ROLE_PERMISSION_REFS = 200;
    private static final Pattern PHYSICAL_ID = Pattern.compile("^[0-9a-fA-F]{32}$");

    private final TenantSessionService sessionService;
    private final ScopedPermissionService scopedPermissionService;
    private final SaasRoleCatalogDao catalogDao;

    public SaasCatalogPageVO roles(int page, int pageSize, String keyword) {
        TenantSessionContext context = requireAdmin();
        Paging paging = validatePaging(page, pageSize, keyword);
        return result(page, pageSize, catalogDao.selectRoles(context.tenantRefId(),
                context.appRefId(), context.year(), paging.keyword(),
                paging.limit(), paging.offset()));
    }

    public SaasCatalogPageVO permissions(int page, int pageSize, String keyword) {
        TenantSessionContext context = requireAdmin();
        Paging paging = validatePaging(page, pageSize, keyword);
        return result(page, pageSize, catalogDao.selectDelegablePermissions(context.tenantRefId(),
                context.appRefId(), context.year(), context.memberRefId(), paging.keyword(),
                paging.limit(), paging.offset()));
    }

    public SaasCatalogPageVO members(int page, int pageSize, String keyword) {
        TenantSessionContext context = requireAdmin();
        Paging paging = validatePaging(page, pageSize, keyword);
        return result(page, pageSize, catalogDao.selectMembers(context.tenantRefId(),
                paging.keyword(), paging.limit(), paging.offset()));
    }

    public SaasCatalogPageVO organizations(int page, int pageSize, String keyword) {
        TenantSessionContext context = requireAdmin();
        Paging paging = validatePaging(page, pageSize, keyword);
        return result(page, pageSize, catalogDao.selectOrganizations(context.tenantRefId(),
                paging.keyword(), paging.limit(), paging.offset()));
    }

    public List<String> rolePermissionRefs(String roleRefId) {
        TenantSessionContext context = requireAdmin();
        requirePhysicalId(roleRefId);
        List<String> stored = catalogDao.selectRolePermissionRefs(
                context.tenantRefId(), context.appRefId(), context.year(), roleRefId);
        if (stored == null) {
            return List.of();
        }
        if (stored.size() > MAX_ROLE_PERMISSION_REFS) {
            throw new SecurityException("Role permission directory exceeds safe limit");
        }
        return List.copyOf(stored);
    }

    public List<SaasCatalogEntryVO> memberBindings(String memberRefId) {
        TenantSessionContext context = requireAdmin();
        requirePhysicalId(memberRefId);
        List<SaasCatalogEntryVO> bindings = catalogDao.selectMemberBindings(
                context.tenantRefId(), context.appRefId(), context.year(), memberRefId);
        if (bindings == null) {
            return List.of();
        }
        if (bindings.size() > MAX_ROLE_PERMISSION_REFS) {
            throw new SecurityException("Member binding directory exceeds safe limit");
        }
        return List.copyOf(bindings);
    }

    private TenantSessionContext requireAdmin() {
        TenantSessionContext context = sessionService.requireCurrent();
        if (context == null || !scopedPermissionService.hasTenantPermission(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                context.year(), MANAGE_PERMISSION)) {
            throw new SecurityException("Current member has no tenant-wide role management grant");
        }
        return context;
    }

    private Paging validatePaging(int page, int pageSize, String keyword) {
        if (page < 1 || page > MAX_PAGE || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Invalid bounded catalog pagination");
        }
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : "";
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("Catalog search keyword exceeds 64 characters");
        }
        return new Paging(normalized, pageSize + 1, (page - 1) * pageSize);
    }

    private SaasCatalogPageVO result(int page, int pageSize, List<SaasCatalogEntryVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return new SaasCatalogPageVO(page, pageSize, false, List.of());
        }
        if (rows.size() > pageSize + 1) {
            throw new SecurityException("Paginated catalog returned more rows than allowed");
        }
        return new SaasCatalogPageVO(page, pageSize, rows.size() > pageSize,
                rows.subList(0, Math.min(pageSize, rows.size())));
    }

    private void requirePhysicalId(String id) {
        if (id == null || !PHYSICAL_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Valid physical target ID is required");
        }
    }

    private record Paging(String keyword, int limit, int offset) {
    }
}
