package com.peach.redis.tool.quickstart.example;

import com.peach.redis.common.tool.RedisDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Set：用户标签集合添加、查询与移除。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:50
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class UserTagSetExample {

    public static final String KEY_PREFIX = "peach:qs:tool:tags:";

    private final RedisDao redisDao;

    public void addTags(String userId, String... tags) {
        String key = KEY_PREFIX + userId;
        for (String tag : tags) {
            redisDao.sAdd(key, tag);
        }
        log.info("tag add, userId={}, count={}", userId, tags.length);
    }

    public Set<Object> listTags(String userId) {
        Set<Object> tags = redisDao.sMembers(KEY_PREFIX + userId);
        log.info("tag list, userId={}, size={}", userId, tags == null ? 0 : tags.size());
        return tags;
    }

    public void removeTag(String userId, String tag) {
        redisDao.sRemove(KEY_PREFIX + userId, tag);
        log.info("tag remove, userId={}, tag={}", userId, tag);
    }

    public void clear(String userId) {
        redisDao.delete(KEY_PREFIX + userId);
    }
}
