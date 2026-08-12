package com.peach.satoken.support;

import com.peach.satoken.constant.SatokenConstant;
import com.peach.satoken.context.UserContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserContextSupport {
    private final StringRedisTemplate redisTemplate;

    public UserContextSupport(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public UserContext findByUserId(String userId) {
        if (isBlank(userId)) {
            return null;
        }
        String key = cacheKey(userId);
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key);
        if (values == null || values.isEmpty() || !userId.equals(value(values, "userId"))) {
            return null;
        }
        UserContext context = new UserContext();
        context.setUserId(userId);
        context.setUserCode(value(values, "userCode"));
        context.setUserName(value(values, "userName"));
        context.setTenantId(value(values, "tenantId"));
        context.setTenantName(value(values, "tenantName"));
        context.setOrgId(value(values, "orgId"));
        context.setOrgCode(value(values, "orgCode"));
        context.setOrgName(value(values, "orgName"));
        context.setFiscal(value(values, "fiscal"));
        context.setLang(value(values, "lang"));
        context.setContextVersion(longValue(values, "contextVersion"));
        return context;
    }

    private String cacheKey(String userId) {
        return SatokenConstant.USER_PROFILE_CACHE_PREFIX + userId;
    }

    private String value(Map<Object, Object> values, String field) {
        Object value = values.get(field);
        return value == null ? null : String.valueOf(value);
    }

    private long longValue(Map<Object, Object> values, String field) {
        String value = value(values, field);
        if (isBlank(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
