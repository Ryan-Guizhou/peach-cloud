package com.peach.monitor.rest;

import com.peach.auth.openfeign.UserFeignClient;
import com.peach.common.response.Response;
import com.peach.monitor.entity.UserDTO;
import com.peach.monitor.entity.monitor.MonitorSnapshotDTO;
import com.peach.monitor.service.IMonitorRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "MonitorController", description = "监控服务接口")
public class MonitorController {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private IMonitorRuntimeService monitorRuntimeService;

    @Operation(summary = "基础联通性检查")
    @GetMapping("/{id}")
    public UserDTO index(@PathVariable("id") String id) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(Long.parseLong(id));
        userDTO.setName("Peach");
        userDTO.setEmail("huanhuanshu48@gmail.com");
        return userDTO;
    }

    @Operation(summary = "获取宿主机/JVM/数据库/中间件监控快照")
    @GetMapping("/snapshot")
    public Response snapshot() {
        return Response.success(monitorRuntimeService.snapshot());
    }

    @Operation(summary = "获取宿主机监控信息")
    @GetMapping("/host")
    public Response hostInfo() {
        MonitorSnapshotDTO snapshot = monitorRuntimeService.snapshot();
        return Response.success(snapshot.getHostInfo());
    }

    @Operation(summary = "获取 JVM 监控信息")
    @GetMapping("/jvm")
    public Response jvmInfo() {
        MonitorSnapshotDTO snapshot = monitorRuntimeService.snapshot();
        return Response.success(snapshot.getJvmInfo());
    }

    @Operation(summary = "获取数据库连通信息")
    @GetMapping("/database")
    public Response databaseInfo() {
        MonitorSnapshotDTO snapshot = monitorRuntimeService.snapshot();
        return Response.success(snapshot.getDatabaseInfo());
    }

    @Operation(summary = "获取中间件连通信息")
    @GetMapping("/middleware")
    public Response middlewareInfo() {
        MonitorSnapshotDTO snapshot = monitorRuntimeService.snapshot();
        return Response.success(snapshot.getMiddlewareInfo());
    }

    @Operation(summary = "获取路由信息")
    @GetMapping("/routerInfo/{routerId}")
    public Response routerInfo(@PathVariable("routerId") String routerId) {
        return userFeignClient.getRouterInfo(routerId);
    }

    @Operation(summary = "获取角色信息")
    @GetMapping("/roleInfo/{roleId}")
    public Response roleInfo(@PathVariable("roleId") String roleId) {
        return userFeignClient.getRoleInfo(roleId);
    }
}
