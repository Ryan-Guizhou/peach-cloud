package com.peach.userservice.launch.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025-11-25 17:47
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("")
    public Map<String,Object> health() {
        Map<String,Object> map = new HashMap<>();
        map.put("status","ok");
        map.put("application","peach-userservice");
        return map;
    }

    @GetMapping("/db")
    public Map<String, Object> dbHealth() {
        Map<String, Object> map = new HashMap<>();
        map.put("application", "peach-userservice");
        try (Connection connection = dataSource.getConnection()) {
            map.put("status", "ok");
            map.put("db", connection.isValid(2) ? "ok" : "invalid");
        } catch (Exception e) {
            map.put("status", "fail");
            map.put("db", "fail");
            map.put("error", e.getMessage());
        }
        return map;
    }


}
