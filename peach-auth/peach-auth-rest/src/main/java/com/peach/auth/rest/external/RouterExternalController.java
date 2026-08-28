package com.peach.auth.rest.external;

import lombok.RequiredArgsConstructor;

import com.peach.auth.service.IRouterService;
import com.peach.common.response.Response;
import com.peach.auth.vo.RouterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 路由External控制器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 17:26
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/auth/external")
@Tag(name = "路由管理外部接口")
@RequiredArgsConstructor
public class RouterExternalController {

        private final IRouterService routerService;

    @GetMapping("/router/{routerId}")
    @Operation(summary = "服务间调用外部接口,根据路由ID查询路由信息")
    public Response selectById(@Parameter(required = true, description = "路由ID")
                               @PathVariable("routerId") String routerId) {

        RouterVO routerVO = routerService.selectById(routerId);
        return Response.success(routerVO);
    }
}
