package com.peach.auth.service;

import com.peach.auth.dao.SaasIamDirectoryDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.service.support.IamPermissionCodes;
import com.peach.auth.vo.SaasIamMemberBriefVO;
import com.peach.auth.vo.SaasIamOrganizationBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 受信服务间成员及机构目录最小批量查询。
 *
 * <p>每次重查 Token 和数据库授权版本，仅具备当前租户完整管理权限的调用者
 * 可读取最小目录。跨租户、无权限、过大请求、重复或不合法物理 ID 全部拒绝；
 * 返回值不含密码摘要或用户联系方式。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:50
 */
@Service
@RequiredArgsConstructor
public class SaasIamRemoteDirectoryService {

    private static final int MAX_BATCH = 100;

    private final TenantSessionService tenantSessionService;

    private final ScopedPermissionService permissionService;

    private final SaasIamDirectoryDao directoryDao;

    /**
     * 批量读取当前租户有效成员，仅管理员可调用。
     */
    @Transactional(readOnly = true)
    public List<SaasIamMemberBriefVO> members(String subjectToken, List<String> ids) {
        TenantSessionContext context = requireManagementScope(
                subjectToken, IamPermissionCodes.TENANT_MEMBER_MANAGE);
        List<String> checked = validateIds(ids);
        return checked.isEmpty() ? List.of() : directoryDao.selectActiveMembers(context.tenantRefId(), checked);
    }

    /**
     * 批量读取当前租户有效机构，仅持有完整租户机构管理权限的调用方可调用。
     */
    @Transactional(readOnly = true)
    public List<SaasIamOrganizationBriefVO> organizations(String subjectToken, List<String> ids) {
        TenantSessionContext context = requireManagementScope(
                subjectToken, IamPermissionCodes.TENANT_ORG_MANAGE);
        List<String> checked = validateIds(ids);
        return checked.isEmpty() ? List.of() : directoryDao.selectActiveOrganizations(
                context.tenantRefId(), checked);
    }

    private TenantSessionContext requireManagementScope(String token, String permissionCode) {
        TenantSessionContext context = tenantSessionService.requireForToken(token);
        if (!permissionService.hasTenantPermission(context.tenantRefId(),
                context.memberRefId(), context.appRefId(), context.year(), permissionCode)) {
            throw new SecurityException("Tenant directory management access is denied");
        }
        return context;
    }

    private List<String> validateIds(List<String> ids) {
        if (ids == null || ids.size() > MAX_BATCH) {
            throw new IllegalArgumentException("Invalid batch size");
        }
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (!StringUtils.hasText(id) || id.length() > 32 || !seen.add(id)) {
                throw new IllegalArgumentException("Invalid or duplicate physical identifier");
            }
        }
        return List.copyOf(ids);
    }
}
