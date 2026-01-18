package com.peach.userservice.rest.internal;

import com.peach.userservice.service.IMenuService;
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
@RequestMapping("/user/menu")
@Tag(name = "MenuController", description = "菜单管理")
public class MenuController {

    @Resource
    private IMenuService menuService;
}
