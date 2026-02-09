package com.peach.monitor.rest;


import com.peach.auth.openfeign.UserFeignClient;
import com.peach.common.response.Response;
import com.peach.monitor.entity.UserDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/8 14:08
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @Resource
    private UserFeignClient userFeignClient;

    @GetMapping("/{id}")
    public UserDTO index(@PathVariable("id") String id) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(Long.parseLong(id));
        userDTO.setName("Peach");
        userDTO.setEmail("huanhuanshu48@gmail.com");
        return userDTO;
    }

    @Operation(summary = "获取路由信息")
    @GetMapping("/routerInfo/{routerId}")
    public Response routerInfo(@PathVariable("routerId") String routerId) {
        Response routerInfo = userFeignClient.getRouterInfo(routerId);
        return routerInfo;
    }

    @Operation(summary = "获取路由信息")
    @GetMapping("/roleInfo/{roleId}")
    public Response roleInfo(@PathVariable("roleId") String roleId) {
        Response routerInfo = userFeignClient.getRoleInfo(roleId);
        return routerInfo;
    }
}
