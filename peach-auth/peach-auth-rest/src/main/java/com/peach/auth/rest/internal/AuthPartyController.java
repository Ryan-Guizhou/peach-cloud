package com.peach.auth.rest.internal;

import com.peach.auth.dto.UserRoleAuthDTO;
import com.peach.auth.qo.AuthPartyQO;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 旧 ROLE_CODE 用户角色接口默认拒绝，阻断跨租户、跨年度权限扩散。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Indexed
@Validated
@RestController
@RequestMapping("/auth/authParty")
@Tag(name = "AuthPartyController", description = "已停用的旧成员角色接口")
public class AuthPartyController {

    @PostMapping("/listUserRoles")
    @Operation(summary = "旧用户角色查询已停用")
    public Response listUserRoles(@RequestBody AuthPartyQO query) {
        return denied();
    }

    @PostMapping("/saveUserRoles")
    @Operation(summary = "旧用户角色绑定已停用")
    public Response saveUserRoles(@Validated @RequestBody UserRoleAuthDTO request) {
        return denied();
    }

    private static Response denied() {
        return Response.fail(StatusEnum.FORBIDDEN).setMsg("旧角色绑定已停用，请使用 SaaS 成员角色绑定");
    }
}
