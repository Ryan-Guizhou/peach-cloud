package com.peach.satoken.support;

import com.peach.satoken.constant.SatokenConstant;
import com.peach.satoken.context.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.Map;

/**
 * 用户上下文缓存支持类。
 *
 * <p>负责从 Redis 缓存中加载当前用户的上下文信息（包括用户ID、编码、姓名、租户、组织、会计期间、语言等），
 * 缓存 Key 格式为 {@link SatokenConstant#USER_PROFILE_CACHE_PREFIX} + userId，使用 Hash 结构存储字段。
 * 若缓存缺失或数据不完整（如 userId 不匹配），则返回 {@code null}。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Slf4j
@Indexed
@Component
public class UserContextSupport {

    private final StringRedisTemplate redisTemplate;

    public UserContextSupport(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 根据用户ID查询并构建用户上下文对象。
     *
     * <p>从 Redis 中读取指定 Key 的 Hash 数据，校验 userId 是否一致，
     * 若成功则封装为 {@link UserContext} 对象，否则返回 {@code null}。</p>
     *
     * @param userId 用户ID（不可为空，为空时直接返回 {@code null}）
     * @return 用户上下文对象，若缓存不存在或数据无效则返回 {@code null}
     */
    public UserContext getUserContextByUserId(String userId) {
        if (isBlank(userId)) {
            log.warn("getUserContextByUserId called with empty/null userId, return null");
            return null;
        }

        String key = cacheKey(userId);
        log.debug("Loading user context from redis, userId={}", userId);

        Map<Object, Object> values = redisTemplate.opsForHash().entries(key);

        if (values == null || values.isEmpty()) {
            log.debug("User context cache miss, userId={}", userId);
            return null;
        }

        // 校验缓存中 userId 是否匹配，防止脏数据
        String cachedUserId = value(values, SatokenConstant.USER_PROFILE_FIELD_USER_ID);
        if (!userId.equals(cachedUserId)) {
            log.warn("User ID mismatch in user context cache, expected={}, actual={}",
                    userId, cachedUserId);
            return null;
        }

        UserContext context = new UserContext();
        context.setUserId(userId);
        context.setUserCode(value(values, SatokenConstant.USER_PROFILE_FIELD_USER_CODE));
        context.setUserName(value(values, SatokenConstant.USER_PROFILE_FIELD_USER_NAME));
        context.setTenantId(value(values, SatokenConstant.USER_PROFILE_FIELD_TENANT_ID));
        context.setTenantName(value(values, SatokenConstant.USER_PROFILE_FIELD_TENANT_NAME));
        context.setOrgId(value(values, SatokenConstant.USER_PROFILE_FIELD_ORG_ID));
        context.setOrgCode(value(values, SatokenConstant.USER_PROFILE_FIELD_ORG_CODE));
        context.setOrgName(value(values, SatokenConstant.USER_PROFILE_FIELD_ORG_NAME));
        context.setFiscal(value(values, SatokenConstant.USER_PROFILE_FIELD_FISCAL));
        context.setLang(value(values, SatokenConstant.USER_PROFILE_FIELD_LANG));
        context.setContextVersion(longValue(values, SatokenConstant.USER_PROFILE_FIELD_CONTEXT_VERSION));

        log.debug("User context loaded successfully, userId: {}", userId);
        return context;
    }

    /**
     * 构建 Redis 缓存 Key。
     *
     * @param userId 用户ID
     * @return 完整的缓存Key
     */
    private String cacheKey(String userId) {
        return SatokenConstant.USER_PROFILE_CACHE_PREFIX + userId;
    }

    /**
     * 从 Hash 中安全获取字符串值。
     *
     * @param values Redis Hash 结果集
     * @param field  字段名
     * @return 字段对应的字符串值，若不存在则返回 {@code null}
     */
    private String value(Map<Object, Object> values, String field) {
        Object value = values.get(field);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 从 Hash 中安全获取 long 值。
     *
     * @param values Redis Hash 结果集
     * @param field  字段名
     * @return 字段对应的 long 值，若转换失败或不存在则返回 0L
     */
    private long longValue(Map<Object, Object> values, String field) {
        String value = value(values, field);
        if (isBlank(value)) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long value for field '{}', value: '{}', return default 0L",
                    field, value, e);
            return 0L;
        }
    }

    /**
     * 判断字符串是否为空或仅包含空白字符。
     *
     * @param value 待判断字符串
     * @return {@code true} 若为空或空白，否则 {@code false}
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
