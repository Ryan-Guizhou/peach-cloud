package com.peach.fileservice.launch.health;

import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
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
@Slf4j
@Indexed
@RestController
@RequestMapping("/file")
@Tag(name = "HealthController", description = "健康检查")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "文件服务监控健康检查")
    public Response health() {
        Map<String,Object> map = new HashMap<>();
        map.put("status","ok");
        map.put("application","peach-fileservice");
        return Response.success(map);
    }


}
