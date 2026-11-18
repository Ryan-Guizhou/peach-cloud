package com.peach.auth.rest.internal;

import com.peach.auth.dto.SaasTenantCreateDTO;
import com.peach.auth.service.SaasTenantProvisioningService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 受保护的平台租户开通入口。
 *
 * <p>经过 SaaS 会话过滤器的 Same-Token、登录态和数据库授权版本校验后，
 * Service 再严格验证 PLATFORM 控制租户及不可委派的 SYSTEM 创建权限。
 * 不暴露匿名初始化、操作人覆盖或指定来源租户参数。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 18:45
 */
@Indexed
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/saas/platform/tenants")
@Tag(name = "SaasTenantProvisioningController", description = "平台受控租户开通")
public class SaasTenantProvisioningController {

    private final SaasTenantProvisioningService provisioningService;

    @PostMapping
    @Operation(summary = "由可信平台管理员开通租户和首位所有者")
    public Response create(@Valid @RequestBody SaasTenantCreateDTO request) {
        return Response.success(provisioningService.create(request));
    }
}
