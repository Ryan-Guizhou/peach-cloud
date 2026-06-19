package com.peach.setting.config;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

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
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/29 20:51
 * @Description Sa-Token Redis 持久化实现
 */
@Slf4j
@Component
public class CustomSaTokenDao implements SaTokenDao {

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 是否已初始化成功。
     */
    public boolean isInit = false;

    /**
     * ObjectMapper 对象，便于后续二次配置。
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

    @Autowired
    private JedisConnectionFactory jedisConnectionFactory;

    /**
     * 初始化 RedisTemplate 与 ObjectMapper 配置。
     */
    @PostConstruct
    public void init() {
        if (this.isInit) {
            return;
        }

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

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
            log.error("CustomSaTokenDao init failed." + e.getMessage(), e);
        }

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
        this.isInit = true;
    }

    /**
     * 获取字符串值。
     *
     * @param key 键名
     * @return 键值
     */
    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 写入字符串值并设置过期时间。
     *
     * @param key 键名
     * @param value 值
     * @param timeout 过期时间，单位秒
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
     * 更新字符串值，保留原有过期时间。
     *
     * @param key 键名
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
     * @param key 键名
     */
    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 获取字符串值剩余过期时间。
     *
     * @param key 键名
     * @return 剩余过期时间，单位秒
     */
    @Override
    public long getTimeout(String key) {
        Long expire = stringRedisTemplate.getExpire(key);
        return expire == null ? 0 : expire;
    }

    /**
     * 更新字符串值过期时间。
     *
     * @param key 键名
     * @param timeout 过期时间，单位秒
     */
    @Override
    public void updateTimeout(String key, long timeout) {
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            long expire = getTimeout(key);
            if (expire == SaTokenDao.NEVER_EXPIRE) {
                log.info("key: [{}] is already set to never expire.", key);
            } else {
                this.set(key, this.get(key), timeout);
            }
            return;
        }
        stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 获取对象值。
     *
     * @param key 键名
     * @return 键值对象
     */
    @Override
    public Object getObject(String key) {
        return objectRedisTemplate.opsForValue().get(key);
    }

    /**
     * 写入对象值并设置过期时间。
     *
     * @param key 键名
     * @param object 值对象
     * @param timeout 过期时间，单位秒
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
     * 更新对象值，保留原有过期时间。
     *
     * @param key 键名
     * @param object 值对象
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
     * @param key 键名
     */
    @Override
    public void deleteObject(String key) {
        objectRedisTemplate.delete(key);
    }

    /**
     * 获取对象值剩余过期时间。
     *
     * @param key 键名
     * @return 剩余过期时间，单位秒
     */
    @Override
    public long getObjectTimeout(String key) {
        Long expire = objectRedisTemplate.getExpire(key);
        return expire == null ? 0 : expire;
    }

    /**
     * 更新对象值过期时间。
     *
     * @param key 键名
     * @param timeout 过期时间，单位秒
     */
    @Override
    public void updateObjectTimeout(String key, long timeout) {
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            long expire = getObjectTimeout(key);
            if (expire == SaTokenDao.NEVER_EXPIRE) {
                log.info("key: [{}] is already set to never expire", key);
            } else {
                this.setObject(key, this.getObject(key), timeout);
            }
            return;
        }
        objectRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 按关键字检索数据。
     *
     * @param prefix 前缀
     * @param keyword 关键字
     * @param start 开始下标
     * @param size 返回数量，-1 表示取到末尾
     * @param sortType 排序方式，true 升序，false 降序
     * @return 键列表
     */
    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        Set<Object> sets = Collections.singleton(objectRedisTemplate.keys(prefix + "*" + keyword + "*"));
        List<Object> list = new ArrayList<>(sets);
        if (!sortType) {
            Collections.reverse(list);
        }
        start = start < 0 ? 0 : start;
        int end = size == -1 ? list.size() : start + size;
        List<String> resultList = new ArrayList<>();
        for (int i = start; i < end; i++) {
            if (i >= list.size()) {
                return resultList;
            }
            resultList.add(String.valueOf(list.get(i)));
        }
        return resultList;
    }
}
