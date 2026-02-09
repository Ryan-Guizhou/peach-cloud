package com.peach.auth.config;

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
 */
@Slf4j
@Component
public class CustomSaTokenDao implements SaTokenDao {
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 标记：是否已初始化成功 / Whether it has been initialized successfully
     */
    public boolean isInit = false;

    /**
     * ObjectMapper 对象 (以 public 作用域暴露出此对象，方便开发者二次更改配置) / ObjectMapper object (expose this object in public scope to facilitate developers' secondary configuration changes)
     */
    public ObjectMapper objectMapper;

    /**
     * String 读写专用 / String read
     */
    public StringRedisTemplate stringRedisTemplate;

    /**
     * Object 读写专用 / Object read
     */
    public RedisTemplate<String, Object> objectRedisTemplate;

    @Autowired
    private JedisConnectionFactory jedisConnectionFactory;

    /**
     * 初始化 / init
     */
    @PostConstruct
    public void init() {
        // 如果已经初始化成功了，就立刻退出，不重复初始化 / If it has already been initialized, exit immediately, do not repeat the initialization
        if (this.isInit) {
            return;
        }

        // 指定相应的序列化方案 / Specify the serialization scheme
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        // 通过反射获取Mapper对象, 增加一些配置, 增强兼容性 / Get the Mapper object through reflection, add some configurations, enhance compatibility
        try {
            Field field = GenericJackson2JsonRedisSerializer.class.getDeclaredField("mapper");
            field.setAccessible(true);
            this.objectMapper = (ObjectMapper) field.get(valueSerializer);

            // 配置[忽略未知字段] / Ignore unknown fields
            this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // 配置[时间类型转换] / Configure [time type conversion]
            JavaTimeModule timeModule = new JavaTimeModule();

            // LocalDateTime序列化与反序列化 / LocalDateTime serialization and deserialization
            timeModule.addSerializer(new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));

            // LocalDate序列化与反序列化 / LocalDate serialization and deserialization
            timeModule.addSerializer(new LocalDateSerializer(DATE_FORMATTER));
            timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DATE_FORMATTER));

            // LocalTime序列化与反序列化 / LocalTime serialization and deserialization
            timeModule.addSerializer(new LocalTimeSerializer(TIME_FORMATTER));
            timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(TIME_FORMATTER));

            this.objectMapper.registerModule(timeModule);

        } catch (Exception e) {
            log.error("CustomSaTokenDao init failed."+e.getMessage(),e);
        }
        // 构建StringRedisTemplate / Build StringRedisTemplate
        StringRedisTemplate stringTemplate = new StringRedisTemplate();
        stringTemplate.setConnectionFactory(jedisConnectionFactory);
        stringTemplate.afterPropertiesSet();

        // 构建RedisTemplate / Build RedisTemplate
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory);
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        // 开始初始化相关组件 / Start initializing related components
        this.stringRedisTemplate = stringTemplate;
        this.objectRedisTemplate = template;

        // 打上标记，表示已经初始化成功，后续无需再重新初始化 / Mark it as initialized successfully, subsequent initialization is not required
        this.isInit = true;
    }


    /**
     * 获取Value，如无返空 / Get value, if none return empty
     * @param key 键名称 / Key name
     * @return 键对应的值 / Key value
     */
    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 写入Value，并设定存活时间 (单位: 秒) / Set value and expire time (in seconds)
     * @param key 键名称 / Key name
     * @param value 值 / Value
     * @param timeout 数据有效期（值大于0时限时存储，值=-1时永久存储，值=0或小于-2时不存储） / Data validity period (value greater than 0 to store for a certain time, value =-1 to store permanently, value = 0 or less than -2 to not store)
     */
    @Override
    public void set(String key, String value, long timeout) {
        if (timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        // 判断是否为永不过期 / Determine whether it is permanent
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            stringRedisTemplate.opsForValue().set(key, value);
        } else {
            stringRedisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        }
    }

    /**
     * 修改指定key-value键值对 (过期时间不变) / Modify the specified key-value pair (with the expiration time unchanged)
     * @param key 键名称 / Key name
     * @param value 值 / Value
     */
    @Override
    public void update(String key, String value) {
        long expire = getTimeout(key);
        // -2 = 无此键 / No such key
        if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        this.set(key, value, expire);
    }

    /**
     * 删除Value / Delete value
     * @param key 键名称 / Key name
     */
    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 获取Value的剩余存活时间 (单位: 秒) / Get the remaining time of Value's remaining survival (unit: seconds)
     * @param key 指定 key / Specified key
     * @return
     */
    @Override
    public long getTimeout(String key) {
        Long expire = stringRedisTemplate.getExpire(key);
        return expire == null ? 0 : expire;
    }

    /**
     * 修改Value的剩余存活时间 (单位: 秒) / Modify the remaining survival time of Value (unit: seconds)
     * @param key 指定 key / Specified key
     * @param timeout 过期时间（单位: 秒） / Expiration time (unit: seconds)
     */
    @Override
    public void updateTimeout(String key, long timeout) {
        // 判断是否想要设置为永久 / Determine whether you want to set it to permanent
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            long expire = getTimeout(key);
            if (expire == SaTokenDao.NEVER_EXPIRE) {
                // 如果其已经被设置为永久，则不作任何处理 / If it has not been set to permanent, set it again
                log.info("key: [{}] is already set to never expire.",key);
            } else {
                // 如果尚未被设置为永久，那么再次set一次 / If it has not been set to permanent, set it again
                this.set(key, this.get(key), timeout);
            }
            return;
        }
        stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }


    /**
     * 获取Object，如无返空 / Get Object, if none, return empty
     * @param key 键名称 / Key name
     * @return 值 / Value
     */
    @Override
    public Object getObject(String key) {
        return objectRedisTemplate.opsForValue().get(key);
    }

    /**
     * 写入Object，并设定存活时间 (单位: 秒) / Set Object and expire time (unit: seconds)
     * @param key 键名称 / Key name
     * @param object 值 / Value
     * @param timeout 存活时间（值大于0时限时存储，值=-1时永久存储，值=0或小于-2时不存储） / Survival time (values greater than 0 are stored for a limited time, values of -1 are stored permanently, and values of 0 or less than -2 are not stored)
     */
    @Override
    public void setObject(String key, Object object, long timeout) {
        if (timeout == 0 || timeout <= SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        // 判断是否为永不过期 / Determine whether it is permanent
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            objectRedisTemplate.opsForValue().set(key, object);
        } else {
            objectRedisTemplate.opsForValue().set(key, object, timeout, TimeUnit.SECONDS);
        }
    }

    /**
     * 更新Object (过期时间不变) / Update Object (expire time unchanged)
     * @param key 键名称 / Key name
     * @param object 值 / Value
     */
    @Override
    public void updateObject(String key, Object object) {
        long expire = getObjectTimeout(key);
        // -2 = 无此键 / No such key
        if (expire == SaTokenDao.NOT_VALUE_EXPIRE) {
            return;
        }
        this.setObject(key, object, expire);
    }

    /**
     * 删除Object / Delete Obejct
     * @param key 键名称 / Key name
     */
    @Override
    public void deleteObject(String key) {
        objectRedisTemplate.delete(key);
    }

    /**
     * 获取Object的剩余存活时间 (单位: 秒) / Get the remaining survival time of the Object (unit: seconds)
     * @param key 指定 key / Specified key
     * @return 剩余存活时间 / Remaining survival time
     */
    @Override
    public long getObjectTimeout(String key) {
        Long expire = objectRedisTemplate.getExpire(key);
        return expire == null ? 0 : expire;
    }

    /**
     * 修改Object的剩余存活时间 (单位: 秒) / Modify the remaining survival time of the Object (unit: seconds)
     * @param key 指定 key / Specified key
     * @param timeout 剩余存活时间 / Remaining survival time
     */
    @Override
    public void updateObjectTimeout(String key, long timeout) {
        // 判断是否想要设置为永久 / Determine whether you want to set it to permanent
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            long expire = getObjectTimeout(key);
            if (expire == SaTokenDao.NEVER_EXPIRE) {
                // 如果其已经被设置为永久，则不作任何处理 / If it has not been set to permanent, set it again
                log.info("key: [{}] is already set to never expire",key);
            } else {
                // 如果尚未被设置为永久，那么再次set一次 / If it has not been set to permanent, set it again
                this.setObject(key, this.getObject(key), timeout);
            }
            return;
        }
        objectRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
    }


    /**
     * 搜索数据 / search data
     * @param prefix 前缀 / prefix
     * @param keyword 关键字 / keyword
     * @param start 开始处索引 / start index
     * @param size 获取数量  (-1代表从 start 处一直取到末尾) / Get quantity (-1 represents getting from start to the end)
     * @param sortType 排序类型（true=正序，false=反序）/ Sort type (true=Ascending, false=Descending)
     * @return
     */
    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        Set<Object> sets = Collections.singleton(objectRedisTemplate.keys(prefix + "*" + keyword + "*"));
        List<Object> list = new ArrayList<>(sets);
        if (!sortType) {
            Collections.reverse(list);
        }
        start = start < 0 ? 0 : start;
        int end = size == -1 ? list.size() :  start + size;
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
