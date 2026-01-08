package com.peach.monitor.rest;


import com.peach.monitor.entity.UserDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/8 14:08
 */
@RestController
@RequestMapping("/monitor")
public class MonitorController {

    @GetMapping("/{id}")
    public UserDTO index(@PathVariable("id") String id) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(Long.parseLong(id));
        userDTO.setName("Peach");
        userDTO.setEmail("huanhuanshu48@gmail.com");
        return userDTO;
    }
}
