package com.peach.satoken.dao;

import cn.dev33.satoken.dao.SaTokenDao;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
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
 * 基于 Redis 的 Sa-Token DAO 实现。
 *
 * <p>负责 Sa-Token 的字符串、对象、过期时间与搜索操作，底层通过项目内 Redis 连接工厂构建模板。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/26
 */
@Slf4j
public class PeachSaTokenDao implements SaTokenDao {

    /**
     * 时间格式化器。
     */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 日期格式化器。
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 日期时间格式化器。
     */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    public void init() {
        if (initialized) {
            return;
        }

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        configureObjectMapper(valueSerializer);

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
     * 配置对象序列化器的 ObjectMapper。
     *
     * @param valueSerializer JSON 序列化器
     */
    private void configureObjectMapper(GenericJackson2JsonRedisSerializer valueSerializer) {
        try {
            Field field = GenericJackson2JsonRedisSerializer.class.getDeclaredField("mapper");
            field.setAccessible(true);
            this.objectMapper = (ObjectMapper) field.get(valueSerializer);
            this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            JavaTimeModule timeModule = new JavaTimeModule();
            timeModule.addSerializer(new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
            timeModule.addSerializer(new LocalDateSerializer(DATE_FORMATTER));
            timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));
            timeModule.addSerializer(new LocalTimeSerializer(TIME_FORMATTER));
            timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));
            this.objectMapper.registerModule(timeModule);
        } catch (Exception e) {
            log.error("PeachSaTokenDao init failed. {}", e.getMessage(), e);
        }
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
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(keys);
        if (!sortType) {
            Collections.reverse(list);
        }
        int fromIndex = Math.max(start, 0);
        int toIndex = size == -1 ? list.size() : Math.min(fromIndex + size, list.size());
        if (fromIndex >= list.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list.subList(fromIndex, toIndex));
    }
}
