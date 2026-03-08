package com.peach.generator.launch.health;

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
@RequestMapping("/generator")
@Tag(name = "HealthController", description = "代码生成服务健康检查")
public class HealthController {


    @Operation(summary = "代码生成服务检查检查")
    @GetMapping("/health")
    public Map<String,Object> health() {
        Map<String,Object> map = new HashMap<>();
        map.put("status","ok");
        map.put("application","peach-generator");
        return map;
    }

}
