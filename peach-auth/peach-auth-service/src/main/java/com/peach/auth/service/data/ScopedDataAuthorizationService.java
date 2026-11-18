package com.peach.auth.service.data;

import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.ScopedFieldRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 业务服务内部的行级和字段级权限受控入口。
 *
 * <p>调用方必须先根据目标对象物理 ID 从可信数据库得到所属租户、应用、
 * 自然年度、所有者、机构和软删除状态，绝不从浏览器请求声明直接构造可信权属。
 * 本服务先核验最新成员 Token，再按相同绑定求行策略交集，最后处理字段效果。
 *
 * <p>列表/分页必须在 SQL 层先限制可见行；本服务的单对象判断不能在分页后
 * 过滤数据，否则会泄漏总量并破坏分页正确性。写业务必须在同一事务中锁定对象
 * 并在执行 UPDATE 前调用 requireWritableFields。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 19:20
 */
@Service
@RequiredArgsConstructor
public class ScopedDataAuthorizationService {

    private final TenantSessionService tenantSessionService;

    private final ScopedDataAuthorizationDao authorizationDao;

    private final OrganizationScopeMatcher organizationScopeMatcher;

    /**
     * 校验对某一真实业务对象的指定动作授权，支持文件和无字段对象。
     *
     * @param permissionCode 服务端定义的动作权限码
     * @param target 由业务数据访问层构造的真实权属描述
     * @throws SecurityException 未通过成员、年度、对象行或角色绑定时
     */
    @Transactional(readOnly = true)
    public void requireObjectAction(String permissionCode, DataObjectDescriptor target) {
        requireGrants(permissionCode, target);
    }

    /**
     * 对受保护对象输出字段过滤与脱敏结果，未知字段不会返回客户端。
     *
     * @param permissionCode 服务端定义的查看动作权限码
     * @param resourceCode 服务端注册的稳定资源类别
     * @param target 真实数据库对象权属
     * @param sourceFields 服务端构造的字段值映射
     * @return 按同一授权证据过滤后的字段映射，空映射表示无可读字段
     */
    @Transactional(readOnly = true)
    public Map<String, Object> readAuthorizedFields(String permissionCode, String resourceCode,
                                                    DataObjectDescriptor target,
                                                    Map<String, ?> sourceFields) {
        AccessSnapshot snapshot = requireGrants(permissionCode, target);
        List<ScopedFieldRuleVO> fields = loadFields(snapshot, permissionCode, resourceCode);
        return FieldPermissionEvaluator.projectRead(sourceFields, snapshot.grants(), fields);
    }

    /**
     * 校验一次修改中每个 JSON 字段，不允许仅校验部分字段后继续执行更新。
     *
     * @param permissionCode 服务端定义的更新动作权限码
     * @param resourceCode 服务端注册的稳定资源类别
     * @param target 业务事务中锁定的数据对象真实权属
     * @param incomingFields 客户端请求对应的全部待修改字段，包含未知字段
     * @throws SecurityException 不具备任何一个字段的明确写权限时
     */
    @Transactional(readOnly = true)
    public void requireWritableFields(String permissionCode, String resourceCode,
                                      DataObjectDescriptor target,
                                      Map<String, ?> incomingFields) {
        AccessSnapshot snapshot = requireGrants(permissionCode, target);
        List<ScopedFieldRuleVO> fields = loadFields(snapshot, permissionCode, resourceCode);
        FieldPermissionEvaluator.requireWrite(incomingFields, snapshot.grants(), fields);
    }

    private AccessSnapshot requireGrants(String permissionCode, DataObjectDescriptor target) {
        if (!StringUtils.hasText(permissionCode) || target == null) {
            throw new SecurityException("Authenticated action and target are required");
        }
        TenantSessionContext context = tenantSessionService.requireCurrent();
        List<RoleDataGrantVO> candidates = authorizationDao.selectDataGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                context.year(), permissionCode);
        List<RoleDataGrantVO> grants = DataScopeEvaluator.matching(
                context, target, candidates, organizationScopeMatcher);
        if (grants.isEmpty()) {
            throw new SecurityException("Action denied for object or tenant scope");
        }
        return new AccessSnapshot(context, grants);
    }

    private List<ScopedFieldRuleVO> loadFields(AccessSnapshot snapshot, String permissionCode,
                                              String resourceCode) {
        if (!StringUtils.hasText(resourceCode)) {
            throw new SecurityException("Protected resource type is required");
        }
        List<String> bindingRefIds = new ArrayList<>();
        for (RoleDataGrantVO grant : snapshot.grants()) {
            if (!bindingRefIds.contains(grant.getBindingRefId())) {
                bindingRefIds.add(grant.getBindingRefId());
            }
        }
        TenantSessionContext context = snapshot.context();
        List<ScopedFieldRuleVO> rules = authorizationDao.selectFieldRules(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                context.year(), permissionCode, resourceCode, bindingRefIds);
        return CollectionUtils.isEmpty(rules) ? List.of() : rules;
    }

    private record AccessSnapshot(TenantSessionContext context, List<RoleDataGrantVO> grants) {
    }
}
