package com.peach.auth.rest.internal;

import com.peach.auth.service.SaasPolicyCatalogService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SaaS 角色行级与字段策略只读目录，受 Same-Token 及实时成员版本保护。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:55
 */
@Indexed
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/saas/roles/catalog")
@Tag(name = "SaasPolicyCatalogController", description = "SaaS受信数据与字段策略查询")
public class SaasPolicyCatalogController {

    private final SaasPolicyCatalogService policyCatalogService;

    @GetMapping("/fields")
    @Operation(summary = "查询当前租户/应用可委派的受控字段目录")
    public Response fields(@NotBlank @RequestParam String resourceCode,
                           @RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "20") int pageSize) {
        return Response.success(policyCatalogService.fields(resourceCode, page, pageSize));
    }

    @GetMapping("/roles/{roleRefId}/permissions/{permissionRefId}/data-policies")
    @Operation(summary = "读取当前角色动作的完整行级范围策略")
    public Response dataPolicies(@NotBlank @PathVariable String roleRefId,
                                 @NotBlank @PathVariable String permissionRefId) {
        return Response.success(policyCatalogService.dataPolicies(roleRefId, permissionRefId));
    }

    @GetMapping("/roles/{roleRefId}/permissions/{permissionRefId}/field-policies")
    @Operation(summary = "读取当前角色动作在受控资源下的字段读写策略")
    public Response fieldPolicies(@NotBlank @PathVariable String roleRefId,
                                  @NotBlank @PathVariable String permissionRefId,
                                  @NotBlank @RequestParam String resourceCode) {
        return Response.success(policyCatalogService.fieldPolicies(roleRefId, permissionRefId, resourceCode));
    }
}
