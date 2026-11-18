package com.peach.auth.rest.external;

import cn.dev33.satoken.same.SaSameUtil;
import com.peach.auth.constant.SaasIamHeaders;
import com.peach.auth.dto.SaasIamPermissionCheckDTO;
import com.peach.auth.service.SaasIamRemoteAuthorizationService;
import com.peach.auth.vo.SaasIamContextVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 跨微服务 IAM 受信入口，不供浏览器直连。
 *
 * <p>与 Servlet 全局配置无关，每次强制核验 Same-Token 服务身份。
 * 最终用户 Token 通过独立头传递，服务端从 Token 和数据库重新加载 SaaS
 * 成员与版本；缺失服务认证、用户 Token 或查询异常均拒绝。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:20
 */
@Indexed
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/external/iam")
public class SaasIamExternalController {

    private final SaasIamRemoteAuthorizationService remoteAuthorizationService;

    /**
     * 实时判断 API 入口动作权限，不代替业务对象行/字段判定。
     */
    @PostMapping("/check-api")
    public ResponseEntity<SaasIamPermissionDecisionVO> checkApi(
            @RequestHeader(value = SaasIamHeaders.SUBJECT_TOKEN, required = false) String subjectToken,
            @Valid @RequestBody SaasIamPermissionCheckDTO request) {
        try {
            SaSameUtil.checkCurrentRequestToken();
            boolean permitted = remoteAuthorizationService.hasApiPermission(
                    subjectToken, request.getPermissionCode());
            return ResponseEntity.ok(new SaasIamPermissionDecisionVO(permitted));
        } catch (RuntimeException exception) {
            // 绝不返回鉴权失败详情、用户 Token 或权限目录存在性。
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new SaasIamPermissionDecisionVO(false));
        }
    }

    /**
     * 只对已验证 Same-Token 且持有合法最终用户 Token 的服务返回最小物理身份。
     */
    @PostMapping("/context")
    public ResponseEntity<SaasIamContextVO> currentContext(
            @RequestHeader(value = SaasIamHeaders.SUBJECT_TOKEN, required = false) String subjectToken) {
        try {
            SaSameUtil.checkCurrentRequestToken();
            return ResponseEntity.ok(remoteAuthorizationService.requireContext(subjectToken));
        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
