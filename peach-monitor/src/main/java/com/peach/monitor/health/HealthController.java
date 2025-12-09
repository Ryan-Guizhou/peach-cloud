package com.peach.monitor.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private NacosConfig config;

    @GetMapping("")
    public Map<String,Object> health() {
        Map<String,Object> map = new HashMap<>();
        map.put("status","ok");
        map.put("application","peach-monitor");
        return map;
    }

    @GetMapping("/change")
    public Map<String,Object> change() {
//        log.info("config={}",config.getCompany1());
//        log.info("config3={}",config.getCompany3());
        Map<String,Object> map = new HashMap<>();
//        map.put("status","ok");
//        map.put("company",config.getCompany1());
        return map;
    }

}
