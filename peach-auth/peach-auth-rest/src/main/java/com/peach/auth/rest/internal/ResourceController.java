package com.peach.auth.rest.internal;

import com.github.pagehelper.PageInfo;
import com.peach.auth.annoation.UserOperLog;
import com.peach.auth.dto.ResourceDTO;
import com.peach.auth.enums.UserLogEnum;
import com.peach.auth.group.ResourceGroup;
import com.peach.auth.qo.ResourceQO;
import com.peach.auth.service.IResouceService;
import com.peach.auth.vo.ResourceVO;
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

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;

/**
 * 资源管理接口。
 *
 * <p>提供资源列表查询、单条查询以及基础增删改入口。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:07
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/auth/resource")
@Tag(name = "ResourceController", description = "资源管理")
public class ResourceController {

    @Resource
    private IResouceService resourceService;

    @PostMapping("/pageList")
    @Operation(summary = "分页查询资源")
    public Response pageList(@RequestBody ResourceQO resourceQO) {
        PageInfo<ResourceVO> pageInfo = resourceService.pageList(resourceQO);
        return Response.success(pageInfo);
    }

    @GetMapping("/selectById")
    @Operation(summary = "根据资源ID查询资源")
    public Response selectById(@NotBlank(message = "资源ID不能为空") String resourceId) {
        ResourceVO resourceVO = resourceService.selectById(resourceId);
        return Response.success(resourceVO);
    }

    @PostMapping("/add")
    @Operation(summary = "新增资源")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增资源信息,资源信息:['+#p0+']'")
    public Response add(@Validated({ResourceGroup.insertGroup.class}) @RequestBody ResourceDTO resourceDTO) {
        resourceService.add(resourceDTO);
        return Response.success();
    }

    @DeleteMapping("/delById")
    @Operation(summary = "根据资源ID删除资源")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.DELETE,
            optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除资源信息,资源ID:['+#p0+']'")
    public Response delById(@NotBlank(message = "资源ID不能为空") String resourceId) {
        resourceService.delById(resourceId);
        return Response.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新资源")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.UPDATE,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'更新资源信息,资源信息:['+#p0+']'")
    public Response update(@Validated({ResourceGroup.updateGroup.class}) @RequestBody ResourceDTO resourceDTO) {
        resourceService.update(resourceDTO);
        return Response.success();
    }
}
