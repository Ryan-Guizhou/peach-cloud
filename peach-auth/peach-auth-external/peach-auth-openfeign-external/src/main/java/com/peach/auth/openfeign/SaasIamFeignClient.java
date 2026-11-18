package com.peach.auth.openfeign;

import com.peach.auth.constant.SaasIamHeaders;
import com.peach.auth.dto.SaasIamPermissionCheckDTO;
import com.peach.auth.openfeign.fallback.SaasIamFeignClientFallbackFactory;
import com.peach.auth.vo.SaasIamContextVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.constant.ServiceNameConstant;
import com.peach.common.constant.ServicePathConstant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * SaaS IAM 最小受信跨服务契约。
 *
 * <p>调用者必须在可信入口获得真实用户 Token，由 Feign 中间件发送独立
 * Same-Token 服务凭证；不得从请求参数构造或修改用户/租户/成员身份。
 * 返回的动作允许不代表对象或字段权限，受保护业务必须继续本地行级校验。
 * 不可在后台线程合成用户 Token 或将此凭证输出到日志。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:35
 */
@FeignClient(
        contextId = ServiceContextConstant.SAAS_IAM_SERVICE_CONTEXT,
        name = ServiceNameConstant.AUTH_SERVICE,
        path = ServicePathConstant.AUTH_PATH_SERVICE,
        fallbackFactory = SaasIamFeignClientFallbackFactory.class
)
public interface SaasIamFeignClient {

    /**
     * 每次从认证服务数据库核验有效 Token 的当前 API 动作权限。
     *
     * @param subjectToken 当前用户的原始 Sa-Token，禁止日志输出
     * @param request METHOD:/path 格式的固定动作权限码
     * @return true 为动作级授权，其他均拒绝
     */
    @PostMapping("/iam/check-api")
    SaasIamPermissionDecisionVO checkApiPermission(
            @RequestHeader(SaasIamHeaders.SUBJECT_TOKEN) String subjectToken,
            @RequestBody SaasIamPermissionCheckDTO request);

    /**
     * 获取服务端按 Token 和数据库确认的物理成员上下文。
     *
     * @param subjectToken 当前用户的原始 Token，禁止用户主动选择其他身份
     * @return 仅含物理主键、年度与授权版本的最小上下文
     */
    @PostMapping("/iam/context")
    SaasIamContextVO requireContext(@RequestHeader(SaasIamHeaders.SUBJECT_TOKEN) String subjectToken);
}
