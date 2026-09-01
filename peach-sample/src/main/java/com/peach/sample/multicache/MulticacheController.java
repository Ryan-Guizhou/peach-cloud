package com.peach.sample.multicache;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import com.peach.redis.common.tool.RedisDao;
import com.peach.redis.manager.MultiCacheManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Multicache控制器。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 15:15
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/multicache")
@RequiredArgsConstructor
public class MulticacheController {


    private final RedisDao redisDao;

    private final MulticacheService multicacheService;

    private final MultiCacheManagerService multiCacheManagerService;

    /**
     * 测试缓存
     * @param userId
     * @return
     */
    @GetMapping("/put/{userId}")
    public MulticacheService.UserDO put(@PathVariable("userId") String userId) {
        return multicacheService.getUser(userId, "shu");
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
        return multicacheService.getManagerUser(userId, "shu");
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
        var cache = multiCacheManagerService.getCache("userCache" + ":" + userId);
        if (cache == null) {
            return false;
        }
        cache.evict(userId + "-" + "shu");
        return true;
    }
}
