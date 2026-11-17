package com.peach.auth.service.support;

import com.peach.auth.vo.LoginTargetVO;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 从已验证身份可访问的租户应用候选中安全选择登录目标。
 *
 * <p>多个目标时不得自动选择；返回 null 表示需要用户明确选择租户和应用。
 * 任一候选以外的用户请求都被拒绝，不从请求推断权限。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 17:50
 */
public final class LoginTargetSelector {

    private LoginTargetSelector() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 选择可信的租户应用目标。
     *
     * @param candidates 数据库校验后的有效候选
     * @param tenantRefId 客户端请求的租户物理主键
     * @param appRefId 客户端请求的应用物理主键
     * @return 合法目标；多候选未指定时为 null
     */
    public static LoginTargetVO select(List<LoginTargetVO> candidates,
                                       String tenantRefId, String appRefId) {
        if (CollectionUtils.isEmpty(candidates)) {
            throw new SecurityException("No active tenant application is available");
        }
        boolean hasTenant = StringUtils.hasText(tenantRefId);
        boolean hasApp = StringUtils.hasText(appRefId);
        if (hasTenant != hasApp) {
            throw new SecurityException("Tenant and application must be selected together");
        }
        if (!hasTenant) {
            return candidates.size() == 1 ? candidates.get(0) : null;
        }
        for (LoginTargetVO candidate : candidates) {
            if (candidate != null && tenantRefId.equals(candidate.getTenantRefId())
                    && appRefId.equals(candidate.getAppRefId())) {
                return candidate;
            }
        }
        throw new SecurityException("Selected tenant application is not accessible");
    }
}
