package com.peach.redis.tool.quickstart.example;

import com.peach.redis.tool.RedisDao;
import com.peach.redis.tool.quickstart.config.RedisToolDemoContext;
import com.peach.redis.tool.quickstart.enums.RedisToolKey;
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

    private final RedisDao redisDao;

    private final RedisToolDemoContext context;

    public void saveProfile(String userId, String nickname, String city) {
        String key = context.key(RedisToolKey.PROFILE, userId);
        Map<Object, Object> fields = new LinkedHashMap<>();
        fields.put("nickname", nickname);
        fields.put("city", city);
        redisDao.hmSetAll(key, fields);
        log.info("profile save, userId={}", userId);
    }

    public String getNickname(String userId) {
        Object value = redisDao.hmGet(context.key(RedisToolKey.PROFILE, userId), "nickname");
        String nickname = value == null ? null : String.valueOf(value);
        log.info("profile getNickname, userId={}, hit={}", userId, nickname != null);
        return nickname;
    }

    public Map<Object, Object> getAll(String userId) {
        return redisDao.hmGet(context.key(RedisToolKey.PROFILE, userId));
    }

    public void removeCity(String userId) {
        redisDao.hmDelete(context.key(RedisToolKey.PROFILE, userId), "city");
        log.info("profile removeCity, userId={}", userId);
    }

    public void deleteProfile(String userId) {
        redisDao.delete(context.key(RedisToolKey.PROFILE, userId));
    }
}
