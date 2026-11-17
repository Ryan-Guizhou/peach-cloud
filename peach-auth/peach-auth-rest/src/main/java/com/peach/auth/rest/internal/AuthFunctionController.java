package com.peach.auth.rest.internal;

import com.peach.auth.dto.RoleFunctionAuthDTO;
import com.peach.auth.qo.AuthFunctionQO;
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
 * 旧功能授权入口默认拒绝，禁止再次写入过时的机构年度授权事实源。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/17 18:25
 */
@Indexed
@Validated
@RestController
@RequestMapping("/auth/authFunction")
@Tag(name = "AuthFunctionController", description = "已停用的旧功能授权接口")
public class AuthFunctionController {

    @PostMapping("/pageList")
    @Operation(summary = "旧功能授权分页已停用")
    public Response pageList(@RequestBody AuthFunctionQO query) {
        return denied();
    }

    @PostMapping("/list")
    @Operation(summary = "旧功能授权查询已停用")
    public Response list(@RequestBody AuthFunctionQO query) {
        return denied();
    }

    @PostMapping("/saveRoleFunctions")
    @Operation(summary = "旧功能授权修改已停用")
    public Response saveRoleFunctions(@Validated @RequestBody RoleFunctionAuthDTO request) {
        return denied();
    }

    private static Response denied() {
        return Response.fail(StatusEnum.FORBIDDEN).setMsg("旧功能授权接口已停用");
    }
}
