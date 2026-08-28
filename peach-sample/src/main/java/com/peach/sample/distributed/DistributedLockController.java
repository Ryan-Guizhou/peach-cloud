package com.peach.sample.distributed;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Distributed锁控制器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 10:26
 */
@Indexed
@RestController
@RequestMapping("/distributed")
@RequiredArgsConstructor
public class DistributedLockController {

        private final DistributedService distributedService;


    @PostMapping("/lock")
    public String lock(@RequestBody DistributedLockInfo distributedLockInfo){
        return distributedService.getDistributedLock(distributedLockInfo);
    }

    @PostMapping("/repeat")
    public String repeat(@RequestBody DistributedLockInfo distributedLockInfo){
        return distributedService.getRepeatLimit(distributedLockInfo);
    }
}
