package com.peach.auth.rest.internal;

import com.peach.auth.dto.SaasDataPolicyUpdateDTO;
import com.peach.auth.dto.SaasFieldPolicyUpdateDTO;
import com.peach.auth.service.SaasPolicyManagementService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户角色数据范围和字段权限的受保护管理入口。
 *
 * <p>仅收策略内容和目标物理 ID，操作者身份、租户、应用和由 Service
 * 从当前 Token 强制获取并重新校验；数据库行锁、版本失效与审计必须同事务。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:45
 */
@Indexed
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/saas/roles")
@Tag(name = "SaasPolicyController", description = "SaaS角色数据与字段策略")
public class SaasPolicyController {

    private final SaasPolicyManagementService policyService;

    @PutMapping("/{roleRefId}/permissions/{permissionRefId}/data-policies")
    @Operation(summary = "替换角色动作的完整行级数据策略")
    public Response replaceDataPolicies(@NotBlank @PathVariable String roleRefId,
                                        @NotBlank @PathVariable String permissionRefId,
                                        @Valid @RequestBody SaasDataPolicyUpdateDTO request) {
        policyService.replaceDataPolicies(roleRefId, permissionRefId, request.getPolicies());
        return Response.success();
    }

    @PutMapping("/{roleRefId}/permissions/{permissionRefId}/field-policies")
    @Operation(summary = "替换角色动作的完整字段读写策略")
    public Response replaceFieldPolicies(@NotBlank @PathVariable String roleRefId,
                                         @NotBlank @PathVariable String permissionRefId,
                                         @Valid @RequestBody SaasFieldPolicyUpdateDTO request) {
        policyService.replaceFieldPolicies(roleRefId, permissionRefId,
                request.getResourceCode(), request.getPolicies());
        return Response.success();
    }
}
