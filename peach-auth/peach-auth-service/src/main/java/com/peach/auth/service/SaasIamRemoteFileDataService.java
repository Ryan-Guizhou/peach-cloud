package com.peach.auth.service;

import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.dto.SaasIamFileFieldReadDTO;
import com.peach.auth.dto.SaasIamFileFieldWriteDTO;
import com.peach.auth.dto.SaasIamFileObjectCheckDTO;
import com.peach.auth.service.data.DataObjectDescriptor;
import com.peach.auth.service.data.DataScopeEvaluator;
import com.peach.auth.service.data.FieldPermissionEvaluator;
import com.peach.auth.service.data.FileSaasQueryScopePlanner;
import com.peach.auth.service.data.OrganizationScopeDirectory;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.SaasIamFileQueryScopeVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import com.peach.auth.vo.ScopedFieldRuleVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文件领域的远程行级授权求交：同一有效角色绑定必须同时满足动作、行范围和策略。
 *
 * <p>只有通过独立 Same-Token 的 FileService 可以提交真实数据库对象描述。
 * 不允许最终用户直接构造权限码或物理归属；Auth 每次使用最终用户 Token
 * 重新验证数据库成员、应用、授权和全局密码版本。
 * 本服务对文件行权限及字段查看、脱敏、写入进行独立的同绑定判定。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaasIamRemoteFileDataService {

    private static final Set<String> FILE_ACTIONS =
            Set.of("FILE.LIST", "FILE.READ", "FILE.DOWNLOAD", "FILE.UPLOAD",
                    "FILE.DELETE", "FILE.RESTORE", "FILE.EDIT");

    private final TenantSessionService sessionService;

    private final ScopedDataAuthorizationDao authorizationDao;

    private final OrganizationScopeDirectory organizationScopeMatcher;

    private static final Set<String> WRITABLE_FILE_FIELDS =
            Set.of("displayName", "bizTag", "remark");

    private static final Set<String> ALLOWED_FILE_FIELDS = Set.of(
            "fileId", "fileName", "displayName", "contentType", "fileSize", "fileExt",
            "fileStatus", "bizType", "bizId", "bizTag", "remark", "createdTime");

    /**
     * 由真正的有效角色授权求出可用于文件列表 SQL 的同绑定物理行范围。
     * 缺失权限或组织树不完整时拒绝，不把 org scope 自动扩大为 tenant scope。
     */
    @Transactional(readOnly = true)
    public SaasIamFileQueryScopeVO planListQuery(String subjectToken) {
        TenantSessionContext context = sessionService.requireForToken(subjectToken);
        List<RoleDataGrantVO> grants = authorizationDao.selectDataGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(), "FILE.LIST");
        return FileSaasQueryScopePlanner.plan(context, grants, organizationScopeMatcher);
    }

    /**
     * 仅同一有效角色绑定同时满足 FILE.EDIT 行级范围与所有待更新字段 WRITE=ALLOW
     * 才能修改文件元数据。字段目录和数据对象权属不能从最终用户参数信任。
     *
     * @param subjectToken 当前真正的最终用户 Token
     * @param request 由可信文件服务从数据库获取物理归属后构造的修改字段
     * @return 允许或拒绝，不返回可编辑内部属性
     */
    @Transactional(readOnly = true)
    public SaasIamPermissionDecisionVO checkWriteFields(String subjectToken,
                                                         SaasIamFileFieldWriteDTO request) {
        if (request == null || request.getObject() == null || request.getChanges() == null
                || request.getChanges().isEmpty()
                || !WRITABLE_FILE_FIELDS.containsAll(request.getChanges().keySet())) {
            return new SaasIamPermissionDecisionVO(false);
        }
        SaasIamFileObjectCheckDTO object = request.getObject();
        if (!"FILE.EDIT".equals(object.getPermissionCode()) || object.isDeleted()
                || !StringUtils.hasText(object.getTenantRefId())
                || !StringUtils.hasText(object.getAppRefId())
                || !StringUtils.hasText(object.getObjectRefId())
                || !StringUtils.hasText(object.getOwnerUserRefId())) {
            return new SaasIamPermissionDecisionVO(false);
        }
        TenantSessionContext context = sessionService.requireForToken(subjectToken);
        if (!context.tenantRefId().equals(object.getTenantRefId())
                || !context.appRefId().equals(object.getAppRefId())) {
            return new SaasIamPermissionDecisionVO(false);
        }
        List<RoleDataGrantVO> candidates = authorizationDao.selectDataGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(), "FILE.EDIT");
        List<RoleDataGrantVO> matching = DataScopeEvaluator.matching(
                context, new DataObjectDescriptor(object.getTenantRefId(), object.getAppRefId(),
                        object.getObjectRefId(), object.getOwnerUserRefId(), object.getOrgRefId(), false),
                candidates, organizationScopeMatcher);
        if (matching.isEmpty()) {
            return new SaasIamPermissionDecisionVO(false);
        }
        List<String> bindings = new ArrayList<>();
        for (RoleDataGrantVO grant : matching) {
            if (!bindings.contains(grant.getBindingRefId())) {
                bindings.add(grant.getBindingRefId());
            }
        }
        List<ScopedFieldRuleVO> rules = authorizationDao.selectFieldRules(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                "FILE.EDIT", "FILE_RECORD", bindings);
        FieldPermissionEvaluator.requireWrite(request.getChanges(), matching, rules);
        return new SaasIamPermissionDecisionVO(true);
    }

    /**
     * 批量投影本人文件列表。只查询一次角色 DATA 授权和字段策略，
     * 对每条来自数据库的物理对象独立计算绑定/数据范围交集；任意行失败则整页拒绝。
     *
     * <p>请求数上限 50，禁止将应用/租户维度的泛化授权跨对象重用。</p>
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> projectBatchFields(String subjectToken,
                                                         List<SaasIamFileFieldReadDTO> requests) {
        if (CollectionUtils.isEmpty(requests) || requests.size() > 50) {
            return List.of();
        }
        TenantSessionContext context = sessionService.requireForToken(subjectToken);
        List<RoleDataGrantVO> candidates = authorizationDao.selectDataGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(), "FILE.LIST");
        if (CollectionUtils.isEmpty(candidates) || candidates.size() > 64) {
            return List.of();
        }
        List<List<RoleDataGrantVO>> grantsByRecord = new ArrayList<>(requests.size());
        List<String> bindingRefIds = new ArrayList<>();
        for (SaasIamFileFieldReadDTO request : requests) {
            if (request == null || request.getObject() == null
                    || request.getFields() == null || request.getFields().isEmpty()
                    || request.getFields().size() > ALLOWED_FILE_FIELDS.size()
                    || !ALLOWED_FILE_FIELDS.containsAll(request.getFields().keySet())) {
                return List.of();
            }
            SaasIamFileObjectCheckDTO object = request.getObject();
            if (!"FILE.LIST".equals(object.getPermissionCode()) || object.isDeleted()
                    || !context.tenantRefId().equals(object.getTenantRefId())
                    || !context.appRefId().equals(object.getAppRefId())
                    || !StringUtils.hasText(object.getObjectRefId())
                    || !StringUtils.hasText(object.getOwnerUserRefId())) {
                return List.of();
            }
            DataObjectDescriptor descriptor = new DataObjectDescriptor(
                    object.getTenantRefId(), object.getAppRefId(), object.getObjectRefId(),
                    object.getOwnerUserRefId(), object.getOrgRefId(), false);
            List<RoleDataGrantVO> matching = DataScopeEvaluator.matching(
                    context, descriptor, candidates, organizationScopeMatcher);
            if (matching.isEmpty()) {
                return List.of();
            }
            grantsByRecord.add(matching);
            for (RoleDataGrantVO grant : matching) {
                if (!bindingRefIds.contains(grant.getBindingRefId())) {
                    bindingRefIds.add(grant.getBindingRefId());
                }
            }
        }
        List<ScopedFieldRuleVO> policies = authorizationDao.selectFieldRules(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                "FILE.LIST", "FILE_RECORD", bindingRefIds);
        if (CollectionUtils.isEmpty(policies) || policies.size() > 256) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(requests.size());
        for (int index = 0; index < requests.size(); index++) {
            Map<String, Object> projected = FieldPermissionEvaluator.projectRead(
                    requests.get(index).getFields(), grantsByRecord.get(index), policies);
            if (projected.isEmpty()) {
                return List.of();
            }
            result.add(projected);
        }
        return List.copyOf(result);
    }

    /**
     * 只返回当前用户在同一对象/动作上确实获得的可见或脱敏字段。
     * 缺少数据或字段策略、未知字段、权限后端异常都拒绝而不降级。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> projectFields(String subjectToken, SaasIamFileFieldReadDTO request) {
        if (request == null || request.getObject() == null
                || request.getFields() == null || request.getFields().isEmpty()
                || request.getFields().size() > ALLOWED_FILE_FIELDS.size()
                || !ALLOWED_FILE_FIELDS.containsAll(request.getFields().keySet())) {
            return Map.of();
        }
        SaasIamFileObjectCheckDTO object = request.getObject();
        if (!Set.of("FILE.READ", "FILE.LIST").contains(object.getPermissionCode())
                || !StringUtils.hasText(object.getTenantRefId())
                || !StringUtils.hasText(object.getAppRefId())
                || !StringUtils.hasText(object.getObjectRefId())
                || !StringUtils.hasText(object.getOwnerUserRefId())
                || object.isDeleted()) {
            return Map.of();
        }
        TenantSessionContext context = sessionService.requireForToken(subjectToken);
        if (!object.getTenantRefId().equals(context.tenantRefId())
                || !object.getAppRefId().equals(context.appRefId())) {
            return Map.of();
        }
        List<RoleDataGrantVO> candidates = authorizationDao.selectDataGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                object.getPermissionCode());
        if (CollectionUtils.isEmpty(candidates) || candidates.size() > 64) {
            return Map.of();
        }
        DataObjectDescriptor descriptor = new DataObjectDescriptor(
                object.getTenantRefId(), object.getAppRefId(), object.getObjectRefId(),
                object.getOwnerUserRefId(), object.getOrgRefId(), false);
        List<RoleDataGrantVO> matching = DataScopeEvaluator.matching(
                context, descriptor, candidates, organizationScopeMatcher);
        if (matching.isEmpty()) {
            return Map.of();
        }
        List<String> bindings = new ArrayList<>();
        for (RoleDataGrantVO grant : matching) {
            if (!bindings.contains(grant.getBindingRefId())) {
                bindings.add(grant.getBindingRefId());
            }
        }
        List<ScopedFieldRuleVO> rules = authorizationDao.selectFieldRules(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                object.getPermissionCode(), "FILE_RECORD", bindings);
        if (CollectionUtils.isEmpty(rules) || rules.size() > 256) {
            return Map.of();
        }
        Map<String, Object> whitelisted = new LinkedHashMap<>(request.getFields());
        return FieldPermissionEvaluator.projectRead(whitelisted, matching, rules);
    }

    /**
     * 根据真正的角色/策略绑定判断指定文件对象动作。
     *
     * @param subjectToken 受信服务转交的真实最终用户 Token
     * @param request 受信文件服务从数据库构造的对象事实
     * @return 仅在同一绑定允许时返回 true
     */
    @Transactional(readOnly = true)
    public SaasIamPermissionDecisionVO decide(String subjectToken,
                                               SaasIamFileObjectCheckDTO request) {
        if (request == null || !FILE_ACTIONS.contains(request.getPermissionCode())
                || !StringUtils.hasText(request.getTenantRefId())
                || !StringUtils.hasText(request.getAppRefId())
                || !StringUtils.hasText(request.getObjectRefId())
                || !StringUtils.hasText(request.getOwnerUserRefId())) {
            return new SaasIamPermissionDecisionVO(false);
        }
        TenantSessionContext context = sessionService.requireForToken(subjectToken);
        if (!request.getTenantRefId().equals(context.tenantRefId())
                || !request.getAppRefId().equals(context.appRefId())) {
            return new SaasIamPermissionDecisionVO(false);
        }
        List<RoleDataGrantVO> grants = authorizationDao.selectDataGrants(
                context.tenantRefId(), context.memberRefId(), context.appRefId(),
                request.getPermissionCode());
        if (CollectionUtils.isEmpty(grants)) {
            return new SaasIamPermissionDecisionVO(false);
        }
        DataObjectDescriptor object = new DataObjectDescriptor(
                request.getTenantRefId(), request.getAppRefId(),
                request.getObjectRefId(), request.getOwnerUserRefId(),
                request.getOrgRefId(), request.isDeleted()
                    && !"FILE.RESTORE".equals(request.getPermissionCode()));
        boolean allowed = !DataScopeEvaluator.matching(
                context, object, grants, organizationScopeMatcher).isEmpty();
        if (!allowed) {
            log.debug("File data action denied, action={}", request.getPermissionCode());
        }
        return new SaasIamPermissionDecisionVO(allowed);
    }
}
