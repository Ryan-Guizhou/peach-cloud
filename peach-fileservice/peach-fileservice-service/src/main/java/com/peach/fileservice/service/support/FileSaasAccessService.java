package com.peach.fileservice.service.support;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dto.SaasIamPermissionCheckDTO;
import com.peach.auth.openfeign.SaasIamFeignClient;
import com.peach.auth.vo.SaasIamContextVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import com.peach.fileservice.dao.FileSaasScopeDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

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
     * 用已验证的本次请求最终用户 Token 取得审计成员。
     */
    public String currentOperator() {
        return requireCurrent("POST:/api/file/internal/pageList").memberRefId();
    }
}
