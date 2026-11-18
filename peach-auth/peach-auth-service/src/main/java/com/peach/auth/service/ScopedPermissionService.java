package com.peach.auth.service;

import com.peach.auth.dao.ScopedRoleGrantDao;
import com.peach.auth.vo.RoleScopeGrantVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * SaaS 成员授权事实源，默认拒绝且不从旧机构或角色推断权限。
 *
 * <p>权限查询严格由成员、租户、应用和限定。一次授权必须来自同一条角色绑定，
 * 禁止把不同角色绑定的动作与数据作用域分别求并集造成交叉越权。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:10
 */
@Service
@RequiredArgsConstructor
public class ScopedPermissionService {

    private static final String SCOPE_TENANT = "TENANT";

    private final ScopedRoleGrantDao scopedRoleGrantDao;

    /**
     * 判断某一成员是否持有具备完整租户范围的权限。
     *
     * @param tenantRefId 租户物理主键
     * @param memberRefId 成员物理主键
     * @param appRefId 应用物理主键
     * @param permissionCode 权限语义码
     * @return 存在独立 TENANT 绑定时为 true，否则拒绝
     */
    @Transactional(readOnly = true)
    public boolean hasTenantPermission(String tenantRefId, String memberRefId, String appRefId,
                                       String permissionCode) {
        List<RoleScopeGrantVO> grants = listGrantedBindings(
                tenantRefId, memberRefId, appRefId, permissionCode);
        if (CollectionUtils.isEmpty(grants)) {
            return false;
        }
        for (RoleScopeGrantVO grant : grants) {
            if (grant != null && SCOPE_TENANT.equals(grant.getScopeType())
                    && !StringUtils.hasText(grant.getScopeRefId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询单条授权证据，调用方必须逐条按具体数据对象校验作用域。
     *
     * @param tenantRefId 租户物理主键
     * @param memberRefId 成员物理主键
     * @param appRefId 应用物理主键
     * @param permissionCode 权限语义码
     * @return 独立绑定证据，不代表已经完成任何对象行/字段级授权
     */
    @Transactional(readOnly = true)
    public List<RoleScopeGrantVO> listGrantedBindings(String tenantRefId, String memberRefId,
                                                      String appRefId, String permissionCode) {
        if (!StringUtils.hasText(tenantRefId) || !StringUtils.hasText(memberRefId)
                || !StringUtils.hasText(appRefId) || !StringUtils.hasText(permissionCode)
                ) {
            return List.of();
        }
        List<RoleScopeGrantVO> results = scopedRoleGrantDao.selectGrants(
                tenantRefId, memberRefId, appRefId, permissionCode);
        return results == null ? List.of() : results;
    }
}
