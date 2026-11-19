package com.peach.fileservice.service.support;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dto.SaasIamPermissionCheckDTO;
import com.peach.auth.dto.SaasIamFileObjectCheckDTO;
import com.peach.common.unique.UniqueIdFacade;
import com.peach.fileservice.vo.FileRecordVO;
import com.peach.fileservice.vo.FileUploadSessionVO;
import com.peach.auth.openfeign.SaasIamFeignClient;
import com.peach.auth.vo.SaasIamContextVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import com.peach.fileservice.dao.FileSaasScopeDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;
import java.util.List;

/**
 * 文件操作的受信 IAM 在线授权入口，网络异常一律默认拒绝。
 *
 * <p>只允许以当前 Servlet 请求关联的 Sa-Token 为最终用户凭证；
 * Feign 中间件应独立附加 Same-Token 服务凭证。绝不信任客户端自报
 * tenantRefId、appRefId、memberRefId、角色或业务租户号。
 * 动作允许不等于完整对象授权；对象 SQL 还必须按物理范围及所有者收敛。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileSaasAccessService {

    private static final Pattern ACTION_CODE =
            Pattern.compile("^(GET|POST|PUT|PATCH|DELETE):/api/file/[a-zA-Z0-9/_{}.-]+$");

    private final SaasIamFeignClient iamFeignClient;

    private final FileSaasScopeDao scopeDao;

    /**
     * 受控文件动作，缺失最终用户 Token、远程 IAM 拒绝或数据库范围丢失均拒绝。
     *
     * @param action 业务端内建固定 API 动作权限码，不能来源于用户入参
     * @return 有效且绑定当前租户应用的物理身份快照
     */
    public FileSaasScope requireCurrent(String action) {
        if (!StringUtils.hasText(action) || action.length() > 128
                || !ACTION_CODE.matcher(action).matches()) {
            throw new SecurityException("Invalid trusted file API action");
        }
        String token = StpUtil.getTokenValue();
        if (!StringUtils.hasText(token) || token.length() > 1024) {
            throw new SecurityException("Valid end-user token is required");
        }
        try {
            SaasIamPermissionCheckDTO request = new SaasIamPermissionCheckDTO();
            request.setPermissionCode(action);
            SaasIamPermissionDecisionVO decision = iamFeignClient.checkApiPermission(token, request);
            if (decision == null || !decision.isAllowed() || decision.getContext() == null) {
                throw new SecurityException("File API action was not authorized");
            }
            SaasIamContextVO subject = decision.getContext();
            String businessId = scopeDao.findEnabledTenantBusinessId(subject.getTenantRefId());
            return new FileSaasScope(subject.getTenantRefId(), subject.getAppRefId(),
                    subject.getUserRefId(), subject.getMemberRefId(), businessId);
        } catch (RuntimeException exception) {
            // 不泄漏最终用户 Token、远端权限目录及具体认证错误。
            log.warn("Trusted file authorization rejected");
            throw new SecurityException("Trusted file authorization required");
        }
    }

    /**
     * 对真实文件记录再次执行 Auth 数据范围校验，动作与范围必须来自同一有效绑定。
     *
     * @param action FILE.READ / FILE.DOWNLOAD / FILE.DELETE / FILE.RESTORE
     * @param scope 本次动作授权得到的物理身份
     * @param record 通过 scoped SQL 返回的真实文件行
     */
    public void requireFileData(String action, FileSaasScope scope, FileRecordVO record) {
        if (record == null || !StringUtils.hasText(record.getId())
                || !scope.tenantRefId().equals(record.getTenantRefId())
                || !scope.appRefId().equals(record.getAppRefId())
                || !scope.userRefId().equals(record.getOwnerUserRefId())) {
            throw new SecurityException("File ownership is unavailable or mismatched");
        }
        requireDataDecision(action, scope, record.getId(), record.getOrgRefId(),
                Integer.valueOf(1).equals(record.getIsDelete()));
    }

    /**
     * 上传分片会话的所有者/成员/应用归属及同绑定 FILE.UPLOAD 数据策略。
     */
    public void requireUploadSessionData(FileSaasScope scope, FileUploadSessionVO session) {
        if (session == null || !StringUtils.hasText(session.getId())
                || !scope.tenantRefId().equals(session.getTenantRefId())
                || !scope.appRefId().equals(session.getAppRefId())
                || !scope.memberRefId().equals(session.getMemberRefId())
                || !scope.userRefId().equals(session.getOwnerUserRefId())) {
            throw new SecurityException("Trusted upload session scope mismatch");
        }
        requireDataDecision("FILE.UPLOAD", scope, session.getId(), session.getOrgRefId(), false);
    }

    /**
     * 创建新文件或分页列出本人文件前用服务端构造的虚拟对象校验 FILE 数据策略。
     *
     * <p>未指定机构时只接受独立绑定的 TENANT_ALL 或 SELF 范围授权，
     * ORG/ORG_TREE 不能借此冒充全租户数据范围。</p>
     */
    public void requireOwnDataAction(String action, FileSaasScope scope) {
        if (!List.of("FILE.UPLOAD", "FILE.LIST").contains(action)) {
            throw new SecurityException("Unsupported file data action");
        }
        requireDataDecision(action, scope, UniqueIdFacade.nextId(), null, false);
    }

    private void requireDataDecision(String action, FileSaasScope scope, String objectRefId,
                                     String orgRefId, boolean deleted) {
        if (scope == null || !StringUtils.hasText(objectRefId) || objectRefId.length() > 32) {
            throw new SecurityException("Trusted file object is required");
        }
        String token = StpUtil.getTokenValue();
        if (!StringUtils.hasText(token) || token.length() > 1024) {
            throw new SecurityException("Authenticated file subject is required");
        }
        SaasIamFileObjectCheckDTO request = new SaasIamFileObjectCheckDTO();
        request.setPermissionCode(action);
        request.setTenantRefId(scope.tenantRefId());
        request.setAppRefId(scope.appRefId());
        request.setObjectRefId(objectRefId);
        request.setOwnerUserRefId(scope.userRefId());
        request.setOrgRefId(orgRefId);
        request.setDeleted(deleted);
        try {
            SaasIamPermissionDecisionVO decision = iamFeignClient.checkFileObjectPermission(token, request);
            if (decision == null || !decision.isAllowed()) {
                throw new SecurityException("File data policy rejected");
            }
        } catch (RuntimeException exception) {
            log.warn("Trusted file data policy rejected, action={}", action);
            throw new SecurityException("Trusted file data authorization is required");
        }
    }

    /**
     * 用已验证的本次请求最终用户 Token 取得审计成员。
     */
    public String currentOperator() {
        return requireContext().memberRefId();
    }

    /**
     * 只查询当前用户 Token 的在线物理身份，不能单独用于授予读写动作。
     *
     * @return IAM 在线核验的物理归属及当前用户
     */
    public FileSaasScope requireContext() {
        String token = StpUtil.getTokenValue();
        if (!StringUtils.hasText(token) || token.length() > 1024) {
            throw new SecurityException("Valid end-user token is required");
        }
        try {
            SaasIamContextVO context = iamFeignClient.requireContext(token);
            if (context == null) {
                throw new SecurityException("Trusted IAM context unavailable");
            }
            String businessId = scopeDao.findEnabledTenantBusinessId(context.getTenantRefId());
            return new FileSaasScope(context.getTenantRefId(), context.getAppRefId(),
                    context.getUserRefId(), context.getMemberRefId(), businessId);
        } catch (RuntimeException exception) {
            log.warn("Trusted file context rejected");
            throw new SecurityException("Trusted file identity required");
        }
    }

    /**
     * 文件永久业务 ID 只能是一个安全路径段，拒绝请求路径语法注入。
     */
    public FileSaasScope requireForFile(String method, String fileId, String suffix) {
        if (fileId == null || !fileId.matches("[A-Za-z0-9-]{1,64}")
                || suffix == null || !List.of("", "/url", "/restore").contains(suffix)
                || !List.of("GET", "POST", "DELETE").contains(method)) {
            throw new SecurityException("Invalid file action");
        }
        return requireCurrent(method + ":/api/file/internal/" + fileId + suffix);
    }
}
