package com.peach.userservice.rest;

import com.peach.userservice.service.IRouterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
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
@RequestMapping("/user/router")
@Tag(name = "ResourceController", description = "路由管理管理")
public class RouterController {

    @Resource
    private IRouterService routerService;

}
