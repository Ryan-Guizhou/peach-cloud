package com.peach.auth.service.data;

import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 在业务 SQL 执行前创建当前 Token 对指定操作的行级预筛选参数。
 *
 * <p>必须在调用方的业务事务中生成并立即用于 SELECT/COUNT/EXPORT 或写入 SQL，
 * 不得跨请求缓存或通过 HTTP 暴露内部角色绑定。单对象权限由
 * ScopedDataAuthorizationService 负责，二者均按数据策略和绑定范围求交。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:20
 */
@Service
@RequiredArgsConstructor
public class ScopedDataQueryScopeService {

    private final TenantSessionService tenantSessionService;

    private final ScopedDataAuthorizationDao authorizationDao;

    private final OrganizationScopeDirectory organizationScopeDirectory;

    /**
     * 返回按成员、应用、年度及动作隔离的 SQL 参数，不返回可拼接的 SQL 片段。
     *
     * @param permissionCode 受信业务端预先定义的动作语义码
     * @return 以 Clause 为单位原子组合的安全预筛选参数
     * @throws SecurityException 未登录、授权为空、组织异常或范围超限
     */
    @Transactional(readOnly = true)
    public AuthorizedDataQueryScope requireScope(String permissionCode) {
        if (!StringUtils.hasText(permissionCode)) {
            throw new SecurityException("Protected action is required");
        }
        TenantSessionContext context = tenantSessionService.requireCurrent();
        List<RoleDataGrantVO> grants = authorizationDao.selectDataGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                context.year(), permissionCode);
        return AuthorizedDataQueryScope.from(context, grants, organizationScopeDirectory);
    }
}
