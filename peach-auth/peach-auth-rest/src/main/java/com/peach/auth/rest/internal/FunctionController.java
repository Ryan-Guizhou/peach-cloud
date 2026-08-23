package com.peach.auth.rest.internal;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageInfo;
import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.dto.FunctionDTO;
import com.peach.auth.enums.UserLogEnum;
import com.peach.auth.group.FunctionGroup;
import com.peach.auth.qo.FunctionQO;
import com.peach.auth.service.IFunctionService;
import com.peach.auth.vo.FunctionVO;
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
 * 功能管理接口。
 *
 * <p>提供功能列表查询、单条查询以及基础增删改入口。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:07
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/auth/function")
@Tag(name = "FunctionController", description = "功能管理")
@RequiredArgsConstructor
public class FunctionController {

        private final IFunctionService functionService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询功能")
    public Response pageList(@RequestBody FunctionQO functionQO) {
        PageInfo<FunctionVO> pageInfo = functionService.pageList(functionQO);
        return Response.success(pageInfo);
    }

    @GetMapping("/selectById")
    @Operation(summary = "根据功能ID查询功能")
    public Response selectById(@NotBlank(message = "功能ID不能为空") String funcId) {
        FunctionVO functionVO = functionService.selectById(funcId);
        return Response.success(functionVO);
    }

    @PostMapping("/add")
    @Operation(summary = "新增功能")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增功能信息,功能信息:['+#p0+']'")
    public Response add(@Validated({FunctionGroup.insertGroup.class}) @RequestBody FunctionDTO functionDTO) {
        functionService.add(functionDTO);
        return Response.success();
    }

    @DeleteMapping("/delById")
    @Operation(summary = "根据功能ID删除功能")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除功能信息,功能ID:['+#p0+']'")
    public Response delById(@NotBlank(message = "功能ID不能为空") String funcId) {
        functionService.delById(funcId);
        return Response.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新功能")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'更新功能信息,功能信息:['+#p0+']'")
    public Response update(@Validated({FunctionGroup.updateGroup.class}) @RequestBody FunctionDTO functionDTO) {
        functionService.update(functionDTO);
        return Response.success();
    }
}
