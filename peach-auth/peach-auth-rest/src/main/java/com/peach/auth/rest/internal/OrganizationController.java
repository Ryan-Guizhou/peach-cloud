package com.peach.auth.rest.internal;

import com.peach.auth.dto.OrganizationDTO;
import com.peach.auth.dto.SaasOrganizationCreateDTO;
import com.peach.auth.qo.OrganizationQO;
import com.peach.auth.service.SaasOrganizationManagementService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 现有机构 HTTP 路由的受信 SaaS 入口。
 *
 * <p>保留前端正在调用的 URL，但所有请求改由当前受信租户及角色权限驱动；
 * 完全忽略浏览器自报的 tenantId、orgId 和用户身份，不执行旧任意租户 CRUD。
 * 请求在 SaasAuthSessionFilter 中完成 Same-Token 和当前成员版本核验。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 19:30
 */
@Indexed
@Validated
@RestController
@RequestMapping("/auth/organization")
@Tag(name = "OrganizationController", description = "受控租户机构管理")
@RequiredArgsConstructor
public class OrganizationController {

    private final SaasOrganizationManagementService managementService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询当前租户机构")
    public Response pageList(@RequestBody(required = false) OrganizationQO query) {
        int page = query == null || query.getPageNum() == null ? 1 : query.getPageNum();
        int size = query == null || query.getPageSize() == null ? 10 : query.getPageSize();
        return Response.success(managementService.pageList(page, size));
    }

    @GetMapping("/selectById")
    @Operation(summary = "按物理主键读取当前租户机构")
    public Response selectById(@NotBlank String orgId) {
        return Response.success(managementService.selectById(orgId));
    }

    @PostMapping("/add")
    @Operation(summary = "在当前租户新增机构，不接受租户身份覆盖")
    public Response add(@RequestBody OrganizationDTO request) {
        SaasOrganizationCreateDTO command = new SaasOrganizationCreateDTO();
        command.setOrgCode(request.getOrgCode());
        command.setOrgName(request.getOrgName());
        return Response.success(managementService.create(command));
    }

    @PostMapping("/update")
    @Operation(summary = "仅允许修改当前租户机构名称")
    public Response update(@RequestBody OrganizationDTO request) {
        managementService.rename(request.getId(), request.getOrgName());
        return Response.success();
    }

    @DeleteMapping("/delById")
    @Operation(summary = "经依赖检查后删除当前租户机构")
    public Response delById(@NotBlank String orgId) {
        managementService.retire(orgId);
        return Response.success();
    }
}
