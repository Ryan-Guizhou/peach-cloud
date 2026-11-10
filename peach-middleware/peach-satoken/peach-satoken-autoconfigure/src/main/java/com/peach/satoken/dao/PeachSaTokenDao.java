package com.peach.satoken.dao;

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
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.ClusterTopology;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
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
import java.util.LinkedHashSet;
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

    /** SCAN 单轮迭代提示量，不是返回数量限制。 */
    private static final int SEARCH_SCAN_BATCH_SIZE = 500;
    /** 管理端查询默认保留结果上限，避免大量会话键导致堆内存耗尽。 */
    private static final int DEFAULT_SEARCH_MAX_MATCHES = 50_000;

    private final RedisConnectionFactory redisConnectionFactory;
    private final int searchMaxMatches;

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
     * @param redisConnectionFactory Redis 连接工厂
     */
    public PeachSaTokenDao(RedisConnectionFactory redisConnectionFactory) {
        this(redisConnectionFactory, DEFAULT_SEARCH_MAX_MATCHES);
    }

    /**
     * 创建带可控搜索结果上限的 Sa-Token DAO。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @param searchMaxMatches 管理端搜索允许累积的最大唯一键数量，必须大于零
     */
    public PeachSaTokenDao(RedisConnectionFactory redisConnectionFactory, int searchMaxMatches) {
        this.redisConnectionFactory = java.util.Objects.requireNonNull(redisConnectionFactory, "redisConnectionFactory");
        if (searchMaxMatches < 1) {
            throw new IllegalArgumentException("Sa-Token searchMaxMatches must be positive");
        }
        this.searchMaxMatches = searchMaxMatches;
    }

    /**
     * 保留直接以 JedisConnectionFactory 构造的历史调用方式。
     *
     * @param jedisConnectionFactory Jedis 连接工厂
     */
    public PeachSaTokenDao(JedisConnectionFactory jedisConnectionFactory) {
        this((RedisConnectionFactory) jedisConnectionFactory);
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
        stringTemplate.setConnectionFactory(redisConnectionFactory);
        stringTemplate.afterPropertiesSet();

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
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
        if (size < -1) {
            throw new IllegalArgumentException("Sa-Token search size must be -1 or nonnegative");
        }
        if (size == 0) {
            return List.of();
        }
        Set<String> keys = scanKeys(prefix + "*" + keyword + "*");
        if (keys.isEmpty()) {
            return List.of();
        }
        // SCAN returns no consistent snapshot; sort explicitly for deterministic pagination.
        List<String> list = new ArrayList<>(keys);
        Collections.sort(list);
        if (!sortType) {
            Collections.reverse(list);
        }
        int fromIndex = Math.max(start, 0);
        if (fromIndex >= list.size()) {
            return List.of();
        }
        int toIndex = size == -1 ? list.size() : (int) Math.min((long) fromIndex + size, list.size());
        return new ArrayList<>(list.subList(fromIndex, toIndex));
    }

    /**
     * 替换 Redis KEYS 为游标 SCAN；Cluster 模式扫描全部主节点。
     *
     * <p>返回数据并非在线写入下的一致快照；结果到达上限时抛出异常，
     * 而不是静默返回不完整的会话列表。调用方应使用管理端限频。</p>
     */
    private Set<String> scanKeys(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(SEARCH_SCAN_BATCH_SIZE).build();
        Set<String> keys = new LinkedHashSet<>();
        try {
            if (redisConnectionFactory instanceof JedisConnectionFactory jedisFactory
                    && jedisFactory.getClusterConfiguration() != null) {
                try (RedisClusterConnection connection = redisConnectionFactory.getClusterConnection()) {
                    // Do not scan a former master after Cluster replica promotion.
                    // A failed master can remain present in clusterGetNodes() temporarily.
                    Set<RedisClusterNode> reportedNodes = new LinkedHashSet<>();
                    connection.clusterGetNodes().forEach(reportedNodes::add);
                    Set<RedisClusterNode> activeMasters =
                            new ClusterTopology(reportedNodes).getActiveMasterNodes();
                    if (activeMasters.isEmpty()) {
                        throw new IllegalStateException("No active Redis Cluster masters available for Sa-Token search");
                    }
                    // Fail closed on incomplete slot coverage rather than return partial sessions.
                    for (int slot = 0; slot < 16_384; slot++) {
                        boolean owned = false;
                        for (RedisClusterNode master : activeMasters) {
                            if (master.servesSlot(slot)) {
                                owned = true;
                                break;
                            }
                        }
                        if (!owned) {
                            throw new IllegalStateException("Active Redis Cluster master missing hash slot " + slot);
                        }
                    }
                    for (RedisClusterNode node : activeMasters) {
                        try (Cursor<byte[]> cursor = connection.scan(node, options)) {
                            while (cursor.hasNext()) {
                                String key = stringRedisTemplate.getStringSerializer().deserialize(cursor.next());
                                addSearchResult(keys, key);
                            }
                        }
                    }
                }
            } else {
                try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                    while (cursor.hasNext()) {
                        addSearchResult(keys, cursor.next());
                    }
                }
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Sa-Token Redis SCAN failed", exception);
        }
        return keys;
    }

    private void addSearchResult(Set<String> keys, String key) {
        if (key == null || keys.contains(key)) {
            return;
        }
        if (keys.size() >= searchMaxMatches) {
            throw new IllegalStateException("Sa-Token Redis search result limit exceeded: " + searchMaxMatches);
        }
        keys.add(key);
    }

}
