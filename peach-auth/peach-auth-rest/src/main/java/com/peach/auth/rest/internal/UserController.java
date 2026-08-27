package com.peach.auth.rest.internal;

import com.peach.common.PeachGroup;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.dto.UserDTO;
import com.peach.auth.enums.UserLogEnum;
import com.peach.auth.qo.UserQO;
import com.peach.auth.service.IUserService;
import com.peach.auth.vo.UserVO;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户管理接口。
 *
 * <p>提供用户列表查询、单条查询以及基础增删改入口。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:07
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/auth/user")
@Tag(name = "UserController", description = "用户管理")
@RequiredArgsConstructor
public class UserController {

        private final IUserService userService;

    @Operation(summary = "查询用户列表")
    @PostMapping("/pageList")
    public Response pageList(@RequestBody UserQO userQO) {
        PageInfo<UserVO> pageInfo = userService.pageList(userQO);
        return Response.success(pageInfo);
    }

    @Operation(summary = "根据用户ID查询用户信息")
    @GetMapping("/selectById")
    public Response selectById(@NotBlank(message = "用户ID不能为空") String userId) {
        UserVO userVO = userService.selectUserById(userId);
        return Response.success(userVO);
    }

    @Operation(summary = "新增用户")
    @PostMapping("/add")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增用户信息,用户信息:['+#p0+']'")
    public Response add(@Validated({PeachGroup.InsertGroup.class}) @RequestBody UserDTO userDTO) {
        userService.add(userDTO);
        return Response.success();
    }

    @Operation(summary = "根据用户ID删除用户")
    @DeleteMapping("/delById")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除用户信息,用户ID:['+#p0+']'")
    public Response delById(@NotBlank(message = "用户ID不能为空") String userId) {
        userService.delById(userId);
        return Response.success();
    }

    @Operation(summary = "更新用户")
    @PostMapping("/update")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'更新用户信息,用户信息:['+#p0+']'")
    public Response update(@Validated({PeachGroup.UpdateGroup.class}) @RequestBody UserDTO userDTO) {
        userService.update(userDTO);
        return Response.success();
    }

}