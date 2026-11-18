package com.peach.auth.service;

import com.peach.auth.dao.ScopedRoleGrantDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleScopeGrantVO;
import com.peach.auth.vo.SaasIamContextVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 受信服务间 SaaS 权限事实查询：先重新验证 Token 与成员版本，再查当前动作。
 *
 * <p>从不接受 HTTP 请求中的 tenant/member/app/year 作为鉴权事实；
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

    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD");
    private static final Pattern PATH = Pattern.compile("^/[A-Za-z0-9_./{}-]+$");

    private final TenantSessionService sessionService;

    private final ScopedRoleGrantDao grantDao;

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
        TenantSessionContext context = sessionService.requireForToken(subjectToken);
        String code = normalizedCode(permissionCode);
        if (code == null) {
            return false;
        }
        List<RoleScopeGrantVO> grants = grantDao.selectApiGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                context.year(), code);
        return !CollectionUtils.isEmpty(grants);
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
        return new SaasIamContextVO(context.userRefId(), context.tenantRefId(),
                context.memberRefId(), context.appRefId(), context.year(),
                context.memberVersion(), context.authVersion());
    }

    private String normalizedCode(String permissionCode) {
        if (!StringUtils.hasText(permissionCode) || permissionCode.length() > 128) {
            return null;
        }
        int colon = permissionCode.indexOf(':');
        if (colon < 1 || colon == permissionCode.length() - 1) {
            return null;
        }
        String method = permissionCode.substring(0, colon).toUpperCase(Locale.ROOT);
        String path = permissionCode.substring(colon + 1);
        if (!METHODS.contains(method) || !PATH.matcher(path).matches()
                || path.contains("//") || path.contains("/../") || path.endsWith("/..")
                || path.contains("/./") || path.endsWith("/.")) {
            return null;
        }
        return method + ":" + path;
    }
}
