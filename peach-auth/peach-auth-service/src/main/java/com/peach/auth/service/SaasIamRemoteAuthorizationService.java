package com.peach.auth.service;

import com.peach.auth.dao.ScopedRoleGrantDao;
import com.peach.auth.dao.OrganizationHierarchyDao;
import org.springframework.util.StringUtils;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleScopeGrantVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import com.peach.auth.vo.SaasIamContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 受信服务间 SaaS 权限事实查询：先重新验证 Token 与成员版本，再查当前动作。
 *
 * <p>从不接受 HTTP 请求中的 tenant/member/app 作为鉴权事实；
 * 所有身份仅由原始 Token 中的单一 SaaS 快照及实时数据库校验得到。
 * 此方法仅检查 API 入口动作，任何行/字段/文件操作仍需独立数据权限。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:20
 */
@Service
@RequiredArgsConstructor
public class SaasIamRemoteAuthorizationService {

    /**
     * 平台内建服务路由必须由 SYSTEM API 目录授权。
     * TENANT 权限目录不能使用同名 CODE 重写内建服务的系统安全边界。
     */
    private static final List<String> SYSTEM_ROUTE_PREFIXES = List.of(
            "/api/auth/", "/api/setting/", "/api/file/", "/api/fileservice/",
            "/api/message/", "/api/monitor/", "/api/generator/",
            "/api/scheduled/", "/api/gateway/",
            "/auth/", "/setting/", "/fileservice/", "/message/",
            "/monitor/", "/generator/", "/scheduled/"
    );

    private final TenantSessionService sessionService;

    private final ScopedRoleGrantDao grantDao;

    private final OrganizationHierarchyDao organizationHierarchyDao;

    /**
     * 当前持有者是否存在由数据库确认的 API 类动作权限。
     *
     * <p>不会将角色绑定的 ORG/ORG_TREE 范围升级为全租户数据权限；
     * 业务服务必须在对象 SQL 层完成真实数据范围约束。</p>
     *
     * @param subjectToken 当前最终用户 Token（不可记录明文）
     * @param permissionCode METHOD:/path 格式且与权限目录完全一致
     * @return true 只代表动作级授权已完成
     */
    @Transactional(readOnly = true)
    public boolean hasApiPermission(String subjectToken, String permissionCode) {
        return decideApiPermission(subjectToken, permissionCode).isAllowed();
    }

    /**
     * 单次事务返回动作授权与同一次 Token 重验证的物理成员上下文。
     *
     * <p>只有 allowed=true 才附加上下文，调用方必须比对最初 Token 快照，
     * 以避免租户切换与旧版本身份在不同服务之间交叉使用。</p>
     *
     * @param subjectToken 当前最终用户 Token
     * @param permissionCode 精确 API 动作码
     * @return 不带 Token 的最小权限证据
     */
    @Transactional(readOnly = true)
    public SaasIamPermissionDecisionVO decideApiPermission(String subjectToken, String permissionCode) {
        TenantSessionContext context = sessionService.requireForToken(subjectToken);
        String requestCode = ApiPermissionRouteMatcher.normalizeRequestCode(permissionCode);
        if (requestCode == null) {
            return new SaasIamPermissionDecisionVO(false);
        }
        String method = requestCode.substring(0, requestCode.indexOf(':'));
        List<String> catalog = grantDao.selectVisibleApiCodes(
                context.tenantRefId(), context.appRefId(), method);
        String code = ApiPermissionRouteMatcher.resolve(requestCode, catalog);
        if (code == null) {
            return new SaasIamPermissionDecisionVO(false);
        }
        // 平台服务命名空间绝不接受租户定义的同名资源冒充 SYSTEM API。
        boolean systemOnly = isProtectedPlatformRoute(requestCode);
        List<RoleScopeGrantVO> grants = grantDao.selectApiGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                code, systemOnly);
        if (CollectionUtils.isEmpty(grants)) {
            return new SaasIamPermissionDecisionVO(false);
        }
        return new SaasIamPermissionDecisionVO(true, trustedContext(context));
    }

    /**
     * 仅 Auth 访问本地物理租户表。文件服务通过受信 Feign 获得业务编号，不直连 Auth DB。
     */
    private SaasIamContextVO trustedContext(TenantSessionContext context) {
        String businessId = organizationHierarchyDao.findEnabledTenantBusinessId(context.tenantRefId());
        if (!StringUtils.hasText(businessId)) {
            throw new SecurityException("Active business tenant mapping is required");
        }
        SaasIamContextVO snapshot = new SaasIamContextVO(context.userRefId(), context.tenantRefId(),
                context.memberRefId(), context.appRefId(), context.memberVersion(), context.authVersion());
        snapshot.setTenantBusinessId(businessId);
        return snapshot;
    }

    private boolean isProtectedPlatformRoute(String requestCode) {
        String path = requestCode.substring(requestCode.indexOf(':') + 1);
        for (String prefix : SYSTEM_ROUTE_PREFIXES) {
            if (path.startsWith(prefix) || path.equals(prefix.substring(0, prefix.length() - 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从真实 Token 与数据库重取当前成员身份；不接受客户端声明的租户/成员 ID。
     *
     * @param subjectToken 当前最终用户 Token
     * @return 不含密钥与角色的最小有效身份
     */
    @Transactional(readOnly = true)
    public SaasIamContextVO requireContext(String subjectToken) {
        TenantSessionContext context = sessionService.requireForToken(subjectToken);
        return trustedContext(context);
    }

}
