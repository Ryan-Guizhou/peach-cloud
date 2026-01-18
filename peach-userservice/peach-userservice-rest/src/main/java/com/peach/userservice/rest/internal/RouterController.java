package com.peach.userservice.rest.internal;

import com.github.pagehelper.PageInfo;
import com.peach.common.response.Response;
import com.peach.userservice.dto.RouterDTO;
import com.peach.userservice.enums.UserLogEnum;
import com.peach.userservice.annoation.UserOperLog;
import com.peach.userservice.qo.RouterQO;
import com.peach.userservice.service.IRouterService;
import com.peach.userservice.vo.RouterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:21
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/user")
@Tag(name = "ResourceController", description = "路由管理管理")
public class RouterController {

    @Resource
    private IRouterService routerService;

    @Operation(summary = "根据路由ID查询路由信息")
    @GetMapping("/router/{routerId}")
    public Response selectById(@Parameter(required = true, description = "路由ID")
                               @PathVariable("routerId") String routerId) {
        RouterVO routerVO = routerService.selectById(routerId);
        return Response.success(routerVO);
    }

    @Operation(summary = "分页查询路由信息")
    @PostMapping("/router/pageList")
    public Response pageList(@RequestBody RouterQO routerQO) {
        PageInfo<RouterVO> pageInfo = routerService.pageList(routerQO);
        return Response.success(pageInfo);
    }

    @Operation(summary = "新增路由信息")
    @PostMapping("/router")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.DEBUG, optContent = "'新增路由信息,路由信息:['+#p0+']'")
    public Response add(@RequestBody RouterDTO routerDTO) {
        routerService.add(routerDTO);
        return Response.success();
    }

    @Operation(summary = "根据ID删除路由信息")
    @DeleteMapping("/router/{routerId}")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.DELETE,
                 optLevel = UserLogEnum.LogLevel.ERROR, optContent = "'删除路由信息,路由ID:['+#p0+']'")
    public Response delById(@Parameter(required = true, description = "路由ID")
                            @PathVariable("routerId") String routerId) {
        routerService.delById(routerId);
        return Response.success();
    }

}
