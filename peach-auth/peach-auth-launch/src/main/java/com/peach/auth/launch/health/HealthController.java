package com.peach.auth.launch.health;

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
 * 用户服务健康检查。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/auth")
@Tag(name = "HealthController", description = "用户服务健康检查")
public class HealthController {


    @Operation(summary = "用户服务检查检查")
    @GetMapping("/health")
    public Map<String,Object> health() {
        Map<String,Object> map = new HashMap<>();
        map.put("status","ok");
        map.put("application","peach-userservice");
        return map;
    }

}
