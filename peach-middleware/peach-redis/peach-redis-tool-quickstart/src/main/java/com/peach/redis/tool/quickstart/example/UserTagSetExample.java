package com.peach.redis.tool.quickstart.example;

import com.peach.redis.tool.RedisDao;
import com.peach.redis.tool.quickstart.config.RedisToolDemoContext;
import com.peach.redis.tool.quickstart.enums.RedisToolKey;
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

    private final RedisDao redisDao;

    private final RedisToolDemoContext context;

    public void addTags(String userId, String... tags) {
        String key = context.key(RedisToolKey.TAGS, userId);
        for (String tag : tags) {
            redisDao.sAdd(key, tag);
        }
        log.info("tag add, userId={}, count={}", userId, tags.length);
    }

    public Set<Object> listTags(String userId) {
        Set<Object> tags = redisDao.sMembers(context.key(RedisToolKey.TAGS, userId));
        log.info("tag list, userId={}, size={}", userId, tags == null ? 0 : tags.size());
        return tags;
    }

    public void removeTag(String userId, String tag) {
        redisDao.sRemove(context.key(RedisToolKey.TAGS, userId), tag);
        log.info("tag remove, userId={}, tag={}", userId, tag);
    }

    public void clear(String userId) {
        redisDao.delete(context.key(RedisToolKey.TAGS, userId));
    }
}
