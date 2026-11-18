package com.peach.auth.rest.internal;

import com.peach.auth.service.SaasRoleCatalogService;
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
 * SaaS 当前租户的最小可委派授权目录，只允许受信管理者读取。
 *
 * <p>路径处于 Auth 的 SaaS 会话过滤器保护范围，执行 Same-Token、登录态
 * 与数据库版本核验；所有列表另由 Service 检查 TENANT 范围的角色管理权限。
 * 不接受请求自报 tenant、member、app 。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:30
 */
@Indexed
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/saas/roles/catalog")
@Tag(name = "SaasRoleCatalogController", description = "受控SaaS角色权限目录")
public class SaasRoleCatalogController {

    private final SaasRoleCatalogService catalogService;

    @GetMapping("/roles")
    @Operation(summary = "查询当前租户和应用的有效角色")
    public Response roles(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int pageSize,
                          @RequestParam(required = false) String keyword) {
        return Response.success(catalogService.roles(page, pageSize, keyword));
    }

    @GetMapping("/permissions")
    @Operation(summary = "查询当前管理者持有且可以委派的权限")
    public Response permissions(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "20") int pageSize,
                                @RequestParam(required = false) String keyword) {
        return Response.success(catalogService.permissions(page, pageSize, keyword));
    }

    @GetMapping("/members")
    @Operation(summary = "查询当前租户的有效成员最小信息")
    public Response members(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int pageSize,
                            @RequestParam(required = false) String keyword) {
        return Response.success(catalogService.members(page, pageSize, keyword));
    }

    @GetMapping("/organizations")
    @Operation(summary = "查询当前租户的有效机构最小信息")
    public Response organizations(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int pageSize,
                                  @RequestParam(required = false) String keyword) {
        return Response.success(catalogService.organizations(page, pageSize, keyword));
    }

    @GetMapping("/roles/{roleRefId}/permissions")
    @Operation(summary = "读取当前租户角色的有效权限物理ID清单")
    public Response rolePermissionRefs(@NotBlank @PathVariable String roleRefId) {
        return Response.success(catalogService.rolePermissionRefs(roleRefId));
    }

    @GetMapping("/members/{memberRefId}/bindings")
    @Operation(summary = "读取成员在当前应用下的有效角色范围绑定")
    public Response memberBindings(@NotBlank @PathVariable String memberRefId) {
        return Response.success(catalogService.memberBindings(memberRefId));
    }
}
