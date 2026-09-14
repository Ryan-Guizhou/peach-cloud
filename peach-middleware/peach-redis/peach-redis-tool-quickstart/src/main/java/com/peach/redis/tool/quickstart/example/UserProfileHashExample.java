package com.peach.redis.tool.quickstart.example;

import com.peach.redis.common.tool.RedisDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hash：用户资料字段批量写入与按字段读取/删除。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class UserProfileHashExample {

    public static final String KEY_PREFIX = "peach:qs:tool:profile:";

    private final RedisDao redisDao;

    public void saveProfile(String userId, String nickname, String city) {
        String key = KEY_PREFIX + userId;
        Map<Object, Object> fields = new LinkedHashMap<>();
        fields.put("nickname", nickname);
        fields.put("city", city);
        redisDao.hmSetAll(key, fields);
        log.info("profile save, userId={}", userId);
    }

    public String getNickname(String userId) {
        Object value = redisDao.hmGet(KEY_PREFIX + userId, "nickname");
        String nickname = value == null ? null : String.valueOf(value);
        log.info("profile getNickname, userId={}, hit={}", userId, nickname != null);
        return nickname;
    }

    public Map<Object, Object> getAll(String userId) {
        return redisDao.hmGet(KEY_PREFIX + userId);
    }

    public void removeCity(String userId) {
        redisDao.hmDel(KEY_PREFIX + userId, "city");
        log.info("profile removeCity, userId={}", userId);
    }

    public void deleteProfile(String userId) {
        redisDao.delete(KEY_PREFIX + userId);
    }
}
