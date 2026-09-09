package com.peach.auth.rest.internal;

import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.dto.UserRoleAuthDTO;
import com.peach.auth.enums.UserLogEnum;
import com.peach.auth.qo.AuthPartyQO;
import com.peach.auth.service.IAuthPartyService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户角色绑定。
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/auth/authParty")
@Tag(name = "AuthPartyController", description = "用户角色绑定")
@RequiredArgsConstructor
public class AuthPartyController {

    private final IAuthPartyService authPartyService;

    @PostMapping("/listUserRoles")
    @Operation(summary = "查询用户已绑定角色")
    public Response listUserRoles(@RequestBody AuthPartyQO authPartyQO) {
        return Response.success(authPartyService.listUserRoles(authPartyQO));
    }

    @PostMapping("/saveUserRoles")
    @Operation(summary = "保存用户角色绑定")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.INFO,
            optContent = "'保存用户角色绑定,用户账号:['+#p0.userCode+']'")
    public Response saveUserRoles(@Validated @RequestBody UserRoleAuthDTO userRoleAuthDTO) {
        authPartyService.saveUserRoles(userRoleAuthDTO);
        return Response.success();
    }
}
