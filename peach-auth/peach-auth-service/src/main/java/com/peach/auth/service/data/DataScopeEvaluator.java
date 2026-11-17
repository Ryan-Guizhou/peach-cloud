package com.peach.auth.service.data;

import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.vo.RoleDataGrantVO;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 行级权限安全交集与多绑定并集计算。
 *
 * <p>单条证据首先独立求角色绑定范围与数据策略范围的交集；
 * 若某条证据能覆盖对象才参与最终并集。不能从不同角色拼接操作权限与范围。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
public final class DataScopeEvaluator {

    private static final String TENANT = "TENANT";
    private static final String ORG = "ORG";
    private static final String ORG_TREE = "ORG_TREE";
    private static final String TENANT_ALL = "TENANT_ALL";
    private static final String SELF = "SELF";

    private DataScopeEvaluator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 选出在一个具体对象上完整生效的独立授权证据。
     *
     * @param context 已重新验证的 Token 租户成员上下文
     * @param object 从真实数据库读取的对象权属
     * @param candidates 由数据库查询单个动作所返回的授权证据
     * @param hierarchy 可信机构树匹配器
     * @return 每条独立可授权证据，空列表即拒绝
     */
    public static List<RoleDataGrantVO> matching(TenantSessionContext context,
                                                 DataObjectDescriptor object,
                                                 List<RoleDataGrantVO> candidates,
                                                 OrganizationScopeMatcher hierarchy) {
        if (context == null || object == null || object.deleted()
                || !StringUtils.hasText(object.objectRefId())
                || !Objects.equals(context.tenantRefId(), object.tenantRefId())
                || !Objects.equals(context.appRefId(), object.appRefId())
                || context.year() != object.year()
                || CollectionUtils.isEmpty(candidates)) {
            return List.of();
        }
        List<RoleDataGrantVO> results = new ArrayList<>();
        for (RoleDataGrantVO grant : candidates) {
            if (grant != null
                    && StringUtils.hasText(grant.getBindingRefId())
                    && StringUtils.hasText(grant.getRoleRefId())
                    && StringUtils.hasText(grant.getPermissionRefId())
                    && bindingCovers(grant, object, hierarchy)
                    && policyCovers(grant, context, object, hierarchy)) {
                results.add(grant);
            }
        }
        return List.copyOf(results);
    }

    private static boolean bindingCovers(RoleDataGrantVO grant, DataObjectDescriptor object,
                                         OrganizationScopeMatcher hierarchy) {
        String type = grant.getBindingScopeType();
        String anchor = grant.getBindingOrgRefId();
        if (TENANT.equals(type)) {
            return !StringUtils.hasText(anchor);
        }
        if (!StringUtils.hasText(anchor) || !StringUtils.hasText(object.orgRefId())) {
            return false;
        }
        if (ORG.equals(type)) {
            return anchor.equals(object.orgRefId());
        }
        return ORG_TREE.equals(type) && hierarchy != null
                && hierarchy.contains(object.tenantRefId(), anchor, object.orgRefId());
    }

    private static boolean policyCovers(RoleDataGrantVO grant, TenantSessionContext context,
                                        DataObjectDescriptor object, OrganizationScopeMatcher hierarchy) {
        String type = grant.getDataScope();
        String anchor = grant.getDataOrgRefId();
        if (TENANT_ALL.equals(type)) {
            return !StringUtils.hasText(anchor);
        }
        if (SELF.equals(type)) {
            return !StringUtils.hasText(anchor)
                    && StringUtils.hasText(object.ownerUserRefId())
                    && object.ownerUserRefId().equals(context.userRefId());
        }
        if (!StringUtils.hasText(anchor) || !StringUtils.hasText(object.orgRefId())) {
            return false;
        }
        if (ORG.equals(type)) {
            return anchor.equals(object.orgRefId());
        }
        return ORG_TREE.equals(type) && hierarchy != null
                && hierarchy.contains(object.tenantRefId(), anchor, object.orgRefId());
    }
}
