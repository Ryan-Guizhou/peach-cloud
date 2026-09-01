package com.peach.sample.redis.bloom.controller;

import lombok.extern.slf4j.Slf4j;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import com.peach.redis.bloom.core.BloomFilterService;
import com.peach.redis.bloom.core.BloomStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 布隆控制器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/8 15:20
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/bloom")
@RequiredArgsConstructor
public class BloomController {
    private static final String SAMPLE_ORDER_KEY = "order";


    private final BloomFilterService bloomFilterService;

    @PostConstruct
    public void init() {
        for (int i = 10000; i > 0; i--) {
            bloomFilterService.add("user", i);
        }
        for (int i = 100; i > 0; i--) {
            bloomFilterService.add(SAMPLE_ORDER_KEY, i);
        }
    }


    @GetMapping("/mightContain")
    public Map demo(Integer id) {
        boolean b = bloomFilterService.mightContain("user", id);
        log.info("{}",  b);
        boolean b1 = bloomFilterService.mightContain(SAMPLE_ORDER_KEY, id);
        log.info("{}", "b1 = " + b1);
        Map<String,Object> map = new HashMap<>();
        map.put("b",b);
        map.put("b1",b1);
        return map;
    }

    @GetMapping("/status")
    public void status(){
        BloomStatus user = bloomFilterService.status("user");
        log.info("{}", user.toDetailedString());
        BloomStatus order = bloomFilterService.status(SAMPLE_ORDER_KEY);
        log.info("{}", order.toDetailedString());
    }
}
