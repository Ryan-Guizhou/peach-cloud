package com.peach.sample.multicache;

import com.peach.redis.manager.MultiCacheManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 15:16
 */
@Slf4j
@Component
public class MulticacheService {

    public static final Map<String,UserDO> USER_DB_MAP = new HashMap<>();

    @PostConstruct
    public void init() {
        for (int i = 10000; i > 0; i--) {
            UserDO userDO = new UserDO();
            userDO.setUserId(i + "");
            userDO.setUserName("user" + i);
            userDO.setAddress("address" + i);
            userDO.setSalary("salary" + i);
            USER_DB_MAP.put(i + "", userDO);
        }
    }
    private int counter = 0;
    @Autowired
    private MultiCacheManager multiCacheManager;


    @Cacheable(value = "userCache", unless = "#result == null",key="#id+'-'+#name",sync = false,cacheResolver = "dynamicCacheResolver")
    public UserDO getUser(String id,String name) {
        counter ++;
        log.info("counter = {}",counter);
        return USER_DB_MAP.getOrDefault(id, null);
    }

    public UserDO getManagerUser(String id,String name) {
        String cacheName = "userCache"+":"+id;
        Cache userCache = multiCacheManager.getCache(cacheName);
        Cache.ValueWrapper valueWrapper = userCache.get(cacheName +":"+id+"-"+name);
        if (valueWrapper != null) {
            return (UserDO) valueWrapper.get();
        }
        UserDO userDO = USER_DB_MAP.getOrDefault(id, null);
        userCache.put(id+"-"+name, userDO);
        return userDO;
    }

    @CacheEvict(value = "userCache", key="#id+'-'+#name",cacheResolver = "dynamicCacheResolver")
    public void evict(String id, String name) {

    }

    public void managerEvict(String id, String name) {
        String cacheName = "userCache"+":"+id;
        Cache userCache = multiCacheManager.getCache(cacheName);
        if (userCache!=null) {
           userCache.evict(id+"-"+name);
        }
    }

    @Data
    public static class UserDO implements Serializable {

        private static final long serialVersionUID = -247342486981431698L;

        private String userId;

        private String userName;

        private String address;

        private String salary;
    }
}
