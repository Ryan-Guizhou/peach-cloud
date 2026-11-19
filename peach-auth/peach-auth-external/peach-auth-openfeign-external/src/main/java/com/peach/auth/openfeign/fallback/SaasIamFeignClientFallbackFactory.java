package com.peach.auth.openfeign.fallback;

import com.peach.auth.dto.SaasIamPermissionCheckDTO;
import com.peach.auth.dto.SaasIamFileObjectCheckDTO;
import com.peach.auth.dto.SaasIamBatchLookupDTO;
import com.peach.auth.openfeign.SaasIamFeignClient;
import com.peach.auth.vo.SaasIamContextVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import com.peach.auth.vo.SaasIamMemberBriefVO;
import com.peach.auth.vo.SaasIamOrganizationBriefVO;

import java.util.List;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * IAM Feign 超时、断路器和依赖异常时严格拒绝，不返回空成功响应。
 *
 * <p>不能借助非 IAM 缓存、请求标头或浏览器提交的租户 ID 兜底。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:35
 */
public class SaasIamFeignClientFallbackFactory implements FallbackFactory<SaasIamFeignClient> {

    @Override
    public SaasIamFeignClient create(Throwable cause) {
        return new SaasIamFeignClient() {
            @Override
            public SaasIamPermissionDecisionVO checkApiPermission(
                    String subjectToken, SaasIamPermissionCheckDTO request) {
                return new SaasIamPermissionDecisionVO(false);
            }

            @Override
            public SaasIamPermissionDecisionVO checkFileObjectPermission(
                    String subjectToken, SaasIamFileObjectCheckDTO request) {
                return new SaasIamPermissionDecisionVO(false);
            }

            @Override
            public SaasIamContextVO requireContext(String subjectToken) {
                throw new SecurityException("Trusted IAM context is unavailable");
            }

            @Override
            public List<SaasIamMemberBriefVO> members(String subjectToken, SaasIamBatchLookupDTO request) {
                throw new SecurityException("Trusted IAM member directory is unavailable");
            }

            @Override
            public List<SaasIamOrganizationBriefVO> organizations(
                    String subjectToken, SaasIamBatchLookupDTO request) {
                throw new SecurityException("Trusted IAM organization directory is unavailable");
            }
        };
    }
}
