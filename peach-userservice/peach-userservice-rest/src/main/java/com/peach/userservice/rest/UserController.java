package com.peach.userservice.rest;


import com.peach.userservice.entity.UserDTO;
import com.peach.userservice.external.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/8 11:41
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserFeignClient userFeignClient;

    @GetMapping()
    public Map<String,Object> map(){
        Map<String,Object> map = new HashMap<>();
        map.put("status","ok");
        map.put("application","peach-userservice");
        return map;
    }

    @GetMapping("/{id}")
    public UserDTO get(@PathVariable("id") Long id){
        return userFeignClient.getUser(id);
    }

}
