package com.peach.auth.service;

import com.peach.auth.dao.ScopedDataAuthorizationDao;
import com.peach.auth.dto.SaasIamFileObjectCheckDTO;
import com.peach.auth.dto.SaasIamFileFieldReadDTO;
import com.peach.auth.service.data.FieldPermissionEvaluator;
import com.peach.auth.vo.ScopedFieldRuleVO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import com.peach.auth.service.data.DataObjectDescriptor;
import com.peach.auth.service.data.DataScopeEvaluator;
import com.peach.auth.service.data.OrganizationScopeMatcher;
import com.peach.auth.service.impl.TenantSessionContext;
import com.peach.auth.service.impl.TenantSessionService;
import com.peach.auth.vo.RoleDataGrantVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * 文件领域的远程行级授权求交：同一有效角色绑定必须同时满足动作、行范围和策略。
 *
 * <p>只有通过独立 Same-Token 的 FileService 可以提交真实数据库对象描述。
 * 不允许最终用户直接构造权限码或物理归属；Auth 每次使用最终用户 Token
 * 重新验证数据库成员、应用、授权和全局密码版本。
 * 当前接口是行级证据，不代表已经完成字段查看、编辑与脱敏。</p>
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
                    "FILE.DELETE", "FILE.RESTORE");

    private final TenantSessionService sessionService;

    private final ScopedDataAuthorizationDao authorizationDao;

    private final OrganizationScopeMatcher organizationScopeMatcher;

    private static final Set<String> ALLOWED_FILE_FIELDS = Set.of(
            "fileId", "fileName", "displayName", "contentType", "fileSize", "fileExt",
            "fileStatus", "bizType", "bizId", "bizTag", "remark", "createdTime");

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
