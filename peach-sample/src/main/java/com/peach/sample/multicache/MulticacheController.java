package com.peach.sample.multicache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 15:15
 */
@Slf4j
@RestController
@RequestMapping("/multicache")
public class MulticacheController {

    @Autowired
    private MulticacheService multicacheService;

    @GetMapping("/get/{userId}")
    public MulticacheService.UserDO get(@PathVariable("userId") String userId) {
        MulticacheService.UserDO user = multicacheService.getUser(userId);
        return user;
    }
}
