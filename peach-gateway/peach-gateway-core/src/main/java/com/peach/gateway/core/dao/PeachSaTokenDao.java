package com.peach.gateway.core.dao;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.session.SaSession;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.peach.common.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * PeachSa令牌数据访问。
 * <p>负责 Sa-Token 的字符串、对象、过期时间与搜索操作，底层通过项目内 Redis 连接工厂构建模板。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/10 15:30
 */
@Slf4j
public class PeachSaTokenDao implements SaTokenDao {

    /**
     * 时间格式化器。
     */
    public static final DateTimeFormatter TIME_FORMATTER = DateUtil.TIME_ONLY_FORMATTER;

    /**
     * 日期格式化器。
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateUtil.LOCAL_DATE_FORMATTER;

    /**
     * 日期时间格式化器。
     */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateUtil.LOCAL_DATE_TIME_FORMATTER;

    private final JedisConnectionFactory jedisConnectionFactory;

    /**
     * JSON 反序列化对象。
     */
    public ObjectMapper objectMapper;

    /**
     * 字符串 Redis 模板。
     */
    public StringRedisTemplate stringRedisTemplate;

    /**
     * 对象 Redis 模板。
     */
    public RedisTemplate<String, Object> objectRedisTemplate;

    private boolean initialized = false;

    /**
     * 创建 Sa-Token DAO。
     *
     * @param jedisConnectionFactory Redis 连接工厂
     */
    public PeachSaTokenDao(JedisConnectionFactory jedisConnectionFactory) {
        this.jedisConnectionFactory = jedisConnectionFactory;
    }

    /**
     * 初始化 Redis 模板和 Jackson 序列化配置。
     */
    @PostConstruct
    @Override
    public void init() {
        if (initialized) {
            return;
        }

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = createValueSerializer();

        StringRedisTemplate stringTemplate = new StringRedisTemplate();
        stringTemplate.setConnectionFactory(jedisConnectionFactory);
        stringTemplate.afterPropertiesSet();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        this.stringRedisTemplate = stringTemplate;
        this.objectRedisTemplate = template;
        this.initialized = true;
    }

    /**
     * 创建并配置 JSON 序列化器。
     *
     * @return 配置完成的 JSON 序列化器
     */
    private GenericJackson2JsonRedisSerializer createValueSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        JavaTimeModule timeModule = new JavaTimeModule();
        timeModule.addSerializer(new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
        timeModule.addSerializer(new LocalDateSerializer(DATE_FORMATTER));
        timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));
        timeModule.addSerializer(new LocalTimeSerializer(TIME_FORMATTER));
        timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));
        mapper.registerModule(timeModule);

        this.objectMapper = mapper;
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    /**
     * 获取字符串值。
     *
     * @param key 键
     * @return 值
     */
    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 设置字符串值。
     *
     * @param key     键
     * @param value   值
     * @param timeout 超时时间，单位秒
     */
    @Override
    public void set(String key, String value, long timeout) {
        if (timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            stringRedisTemplate.opsForValue().set(key, value);
        } else {
            stringRedisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        }
    }

    /**
     * 更新字符串值。
     *
     * @param key   键
     * @param value 值
     */
    @Override
    public void update(String key, String value) {
        long expire = getTimeout(key);
        if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        this.set(key, value, expire);
    }

    /**
     * 删除字符串值。
     *
     * @param key 键
     */
    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 获取字符串值的剩余过期时间。
     *
     * @param key 键
     * @return 剩余过期时间
     */
    @Override
    public long getTimeout(String key) {
        Long expire = stringRedisTemplate.getExpire(key);
        return expire == null ? 0 : expire;
    }

    /**
     * 更新字符串值过期时间。
     *
     * @param key     键
     * @param timeout 超时时间，单位秒
     */
    @Override
    public void updateTimeout(String key, long timeout) {
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            long expire = getTimeout(key);
            if (expire != SaTokenDao.NEVER_EXPIRE) {
                this.set(key, this.get(key), timeout);
            }
            return;
        }
        stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取对象值。
     *
     * @param key 键
     * @return 对象值
     */
    @Override
    public Object getObject(String key) {
        return objectRedisTemplate.opsForValue().get(key);
    }

    /**
     * 获取指定类型的对象值。
     *
     * @param key 键
     * @param classType 目标类型
     * @param <T> 目标类型
     * @return 对象值
     */
    @Override
    public <T> T getObject(String key, Class<T> classType) {
        Object value = getObject(key);
        if (value == null || classType == null) {
            return null;
        }
        if (classType.isInstance(value)) {
            return classType.cast(value);
        }
        return objectMapper.convertValue(value, classType);
    }

    /**
     * 设置对象值。
     *
     * @param key     键
     * @param object  对象
     * @param timeout 超时时间，单位秒
     */
    @Override
    public void setObject(String key, Object object, long timeout) {
        if (timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            objectRedisTemplate.opsForValue().set(key, object);
        } else {
            objectRedisTemplate.opsForValue().set(key, object, timeout, TimeUnit.SECONDS);
        }
    }

    /**
     * 更新对象值。
     *
     * @param key    键
     * @param object 对象
     */
    @Override
    public void updateObject(String key, Object object) {
        long expire = getObjectTimeout(key);
        if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        this.setObject(key, object, expire);
    }

    /**
     * 删除对象值。
     *
     * @param key 键
     */
    @Override
    public void deleteObject(String key) {
        objectRedisTemplate.delete(key);
    }

    /**
     * 获取对象值剩余过期时间。
     *
     * @param key 键
     * @return 剩余过期时间
     */
    @Override
    public long getObjectTimeout(String key) {
        Long expire = objectRedisTemplate.getExpire(key);
        return expire == null ? 0 : expire;
    }

    /**
     * 更新对象值过期时间。
     *
     * @param key     键
     * @param timeout 超时时间，单位秒
     */
    @Override
    public void updateObjectTimeout(String key, long timeout) {
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            long expire = getObjectTimeout(key);
            if (expire != SaTokenDao.NEVER_EXPIRE) {
                this.setObject(key, this.getObject(key), timeout);
            }
            return;
        }
        objectRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取 Sa-Token Session。
     *
     * @param sessionId Session ID
     * @return Session
     */
    @Override
    public SaSession getSession(String sessionId) {
        return getObject(sessionId, SaSession.class);
    }

    /**
     * 设置 Sa-Token Session。
     *
     * @param session Session
     * @param timeout 超时时间，单位秒
     */
    @Override
    public void setSession(SaSession session, long timeout) {
        if (session == null) {
            return;
        }
        setObject(session.getId(), session, timeout);
    }

    /**
     * 更新 Sa-Token Session。
     *
     * @param session Session
     */
    @Override
    public void updateSession(SaSession session) {
        if (session == null) {
            return;
        }
        updateObject(session.getId(), session);
    }

    /**
     * 删除 Sa-Token Session。
     *
     * @param sessionId Session ID
     */
    @Override
    public void deleteSession(String sessionId) {
        deleteObject(sessionId);
    }

    /**
     * 获取 Sa-Token Session 剩余过期时间。
     *
     * @param sessionId Session ID
     * @return 剩余过期时间
     */
    @Override
    public long getSessionTimeout(String sessionId) {
        return getObjectTimeout(sessionId);
    }

    /**
     * 更新 Sa-Token Session 过期时间。
     *
     * @param sessionId Session ID
     * @param timeout 超时时间，单位秒
     */
    @Override
    public void updateSessionTimeout(String sessionId, long timeout) {
        updateObjectTimeout(sessionId, timeout);
    }

    /**
     * 按前缀和关键字搜索对象数据。
     *
     * @param prefix    前缀
     * @param keyword   关键字
     * @param start     起始下标
     * @param size      返回数量
     * @param sortType  是否正序
     * @return 匹配的数据列表
     */
    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        Set<String> keys = objectRedisTemplate.keys(prefix + "*" + keyword + "*");
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        List<String> list = new ArrayList<>(keys);
        if (!sortType) {
            Collections.reverse(list);
        }
        int fromIndex = Math.max(start, 0);
        int toIndex = size == -1 ? list.size() : Math.min(fromIndex + size, list.size());
        if (fromIndex >= list.size()) {
            return List.of();
        }
        return new ArrayList<>(list.subList(fromIndex, toIndex));
    }
}
