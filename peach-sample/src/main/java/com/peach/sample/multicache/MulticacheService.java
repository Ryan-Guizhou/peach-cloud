package com.peach.sample.multicache;

import com.peach.redis.manager.MultiCacheManager;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/10 15:16
 */
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

    @Autowired
    private MultiCacheManager multiCacheManager;

//    @Cacheable(value = "userCache", unless = "#result == null",cacheResolver = "21",key="#id",sync = false)
//    @Cacheable(value = "userCache", unless = "#result == null",key="#id",sync = false)
    public UserDO getUser(String id) {
        Cache userCache = multiCacheManager.getCache("userCache");
        Cache.ValueWrapper valueWrapper = userCache.get("userCache" + id);
        if (valueWrapper != null) {
            return (UserDO) valueWrapper.get();
        }
        userCache.put("userCache" + id, USER_DB_MAP.getOrDefault(id, null));
        return USER_DB_MAP.getOrDefault(id, null);
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
