package com.peach.sample.redis.bloom.controller;

import com.peach.redis.bloom.core.BloomFilterService;
import com.peach.redis.bloom.core.BloomStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/8 15:20
 */
@RestController
@RequestMapping("/bloom")
public class BloomController {

    @Autowired
    private BloomFilterService bloomFilterService;

    @PostConstruct
    public void init() {
        for (int i = 10000; i > 0; i--) {
            bloomFilterService.add("user", i);
        }
        for (int i = 100; i > 0; i--) {
            bloomFilterService.add("order", i);
        }
    }


    @GetMapping("/mightContain")
    public Map demo(Integer id) {
        boolean b = bloomFilterService.mightContain("user", id);
        System.out.println( b);
        boolean b1 = bloomFilterService.mightContain("order", id);
        System.out.println("b1 = " + b1);
        Map<String,Object> map = new HashMap<>();
        map.put("b",b);
        map.put("b1",b1);
        return map;
    }

    @GetMapping("/status")
    public void status(){
        BloomStatus user = bloomFilterService.status("user");
        System.out.println( user.toDetailedString());
        BloomStatus order = bloomFilterService.status("order");
        System.out.println(order.toDetailedString());
    }
}
