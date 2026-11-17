package com.peach.auth.rest.internal;

import com.peach.auth.dto.RoleDTO;
import com.peach.auth.qo.RoleQO;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import com.peach.common.PeachGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 旧机构与 FISCAL 角色 REST 入口，明确拒绝并等待前端迁移。
 *
 * <p>禁止基于浏览器参数读写跨租户角色；新角色 CRUD 必须经
 * SaasRoleController 的可信成员上下文和独立年度授权执行。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Indexed
@Validated
@RestController
@RequestMapping("/auth/role")
@Tag(name = "RoleController", description = "已停用的旧角色接口")
public class RoleController {

    @PostMapping("/pageList")
    @Operation(summary = "旧角色分页接口已停用")
    public Response pageList(@RequestBody RoleQO query) {
        return denied();
    }

    @GetMapping("/selectById")
    @Operation(summary = "旧角色详情接口已停用")
    public Response selectById(@NotBlank String roleId) {
        return denied();
    }

    @PostMapping("/add")
    @Operation(summary = "旧角色新增接口已停用")
    public Response add(@Validated(PeachGroup.InsertGroup.class) @RequestBody RoleDTO dto) {
        return denied();
    }

    @DeleteMapping("/delById")
    @Operation(summary = "旧角色删除接口已停用")
    public Response delById(@NotBlank String roleId) {
        return denied();
    }

    @PostMapping("/update")
    @Operation(summary = "旧角色更新接口已停用")
    public Response update(@Validated(PeachGroup.UpdateGroup.class) @RequestBody RoleDTO dto) {
        return denied();
    }

    private static Response denied() {
        return Response.fail(StatusEnum.FORBIDDEN).setMsg("旧角色接口已停用，请使用 SaaS 年度角色接口");
    }
}
