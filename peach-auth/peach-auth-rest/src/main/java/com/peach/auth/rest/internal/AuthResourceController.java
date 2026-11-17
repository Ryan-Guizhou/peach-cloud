package com.peach.auth.rest.internal;

import com.peach.auth.dto.RoleResourceAuthDTO;
import com.peach.auth.qo.AuthResourceQO;
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
 * 旧资源授权入口默认拒绝，禁止无可信成员上下文的角色资源修改。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Indexed
@Validated
@RestController
@RequestMapping("/auth/authResource")
@Tag(name = "AuthResourceController", description = "已停用的旧资源授权接口")
public class AuthResourceController {

    @PostMapping("/pageList")
    @Operation(summary = "旧资源授权分页已停用")
    public Response pageList(@RequestBody AuthResourceQO query) {
        return denied();
    }

    @PostMapping("/list")
    @Operation(summary = "旧资源授权查询已停用")
    public Response list(@RequestBody AuthResourceQO query) {
        return denied();
    }

    @PostMapping("/saveRoleResources")
    @Operation(summary = "旧资源授权修改已停用")
    public Response saveRoleResources(@Validated @RequestBody RoleResourceAuthDTO request) {
        return denied();
    }

    private static Response denied() {
        return Response.fail(StatusEnum.FORBIDDEN).setMsg("旧资源授权接口已停用");
    }
}
