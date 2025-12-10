package com.peach.sample.multicache;

import com.peach.redis.common.RedisDao;
import com.peach.redis.manager.MultiCacheManagerService;
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
    private RedisDao redisDao;

    @Autowired
    private MulticacheService multicacheService;

    @Autowired
    private MultiCacheManagerService multiCacheManagerService;

    /**
     * 测试缓存
     * @param userId
     * @return
     */
    @GetMapping("/put/{userId}")
    public MulticacheService.UserDO put(@PathVariable("userId") String userId) {
        MulticacheService.UserDO user = multicacheService.getUser(userId,"shu");
        return user;
    }

    /**
     * 清除缓存
     * @param userId
     * @return
     */
    @GetMapping("/evict/{userId}")
    public boolean evict(@PathVariable("userId") String userId) {
        multicacheService.evict(userId,"shu");
        return true;
    }

    /**
     * 测试缓存是否生效
     * @return
     */
    @GetMapping("/put")
    public boolean put() {
        redisDao.vSet("multicache:shu", 1L);
        return true;
    }

    @GetMapping("/evict")
    public boolean evict() {
        redisDao.delete("multicache:shu");
        return true;
    }


    /**
     * 测试缓存
     * @param userId
     * @return
     */
    @GetMapping("/manager/put/{userId}")
    public MulticacheService.UserDO managerPut(@PathVariable("userId") String userId) {
        MulticacheService.UserDO user = multicacheService.getManagerUser(userId,"shu");
        return user;
    }

    /**
     * 清除缓存
     * @param userId
     * @return
     */
    @GetMapping("/manager/evict/{userId}")
    public boolean managerEvict(@PathVariable("userId") String userId) {
        multicacheService.managerEvict(userId,"shu");
        return true;
    }


    /**
     * 清除缓存
     * @param userId
     * @return
     */
    @GetMapping("/manager1/evict/{userId}")
    public boolean managerEvict1(@PathVariable("userId") String userId) {
        multiCacheManagerService.getCache("userCache"+":"+userId).evict(userId+"-"+"shu");
        return true;
    }
}
