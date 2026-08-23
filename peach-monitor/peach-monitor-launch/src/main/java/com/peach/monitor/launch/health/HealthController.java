package com.peach.monitor.launch.health;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import com.peach.common.response.Response;
import com.peach.monitor.entity.monitor.MonitorSnapshotDTO;
import com.peach.monitor.service.IMonitorRuntimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 */
@Indexed
@RestController
@RequestMapping("/monitor")
@Tag(name = "HealthController", description = "健康检查")
@RequiredArgsConstructor
public class HealthController {

        private final IMonitorRuntimeService monitorRuntimeService;

    @GetMapping("/health")
    @Operation(summary = "监控健康检查")
    public Response health() {
        MonitorSnapshotDTO snapshot = monitorRuntimeService.snapshot();

        Map<String, Object> healthInfo = new HashMap<String, Object>();
        healthInfo.put("status", "ok");
        healthInfo.put("application", snapshot.getServiceName());
        healthInfo.put("timestamp", snapshot.getTimestamp());
        healthInfo.put("uptimeMs", snapshot.getJvmInfo().get("uptimeMs"));
        healthInfo.put("databaseStatus", snapshot.getDatabaseInfo().get("status"));
        healthInfo.put("middlewareStatus", snapshot.getMiddlewareInfo());
        return Response.success(healthInfo);
    }
}
