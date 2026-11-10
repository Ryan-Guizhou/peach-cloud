

package com.peach.redis.common.tool;

import jakarta.annotation.Nullable;
import com.peach.redis.constant.RedisConstant;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.stereotype.Indexed;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * RedisDao实现类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:39
 */
@Slf4j
@Indexed
@Repository("redisDao")
public class RedisDaoImpl extends AbstractBaseRedisDao<Object, Object> implements RedisDao {

    private static final Integer TRY_TIMES = 3;

    private static final String REDIS_OPERATION_FAILED = "Redis operation failed";

    @Value("${peach.redis.mode:standalone}")
    private String redisMode;
    public RedisDaoImpl(RedisTemplate<?, ?> redisTemplate) {
        super(toObjectTemplate(redisTemplate));
    }

    /**
     * 将统一 RedisTemplate 适配到历史 RedisDao 的 Object key/value 契约。
     *
     * <p>RedisTemplate 的泛型只参与编译期类型检查，实际 key 序列化仍由
     * {@link com.peach.redis.common.RedisConfig} 统一配置为字符串序列化器。</p>
     *
     * @param redisTemplate 应用统一 RedisTemplate
     * @return RedisDao 历史契约使用的 RedisTemplate 视图
     */
    @SuppressWarnings("unchecked")
    private static RedisTemplate<Object, Object> toObjectTemplate(RedisTemplate<?, ?> redisTemplate) {
        return (RedisTemplate<Object, Object>) redisTemplate;
    }

    @Override
    public boolean existsKey(final Object key) {
        Boolean retryFlag = redisTemplate.hasKey(key);
        if (retryFlag == null) {
            return false;
        }
        return retryFlag;
    }


    @Override
    public boolean delete(final Object key) {
        Integer retryNum = 1;
        boolean flag = false;
        while (retryNum <= TRY_TIMES) {
            try {
                flag = Boolean.TRUE.equals(redisTemplate.delete(key));
                if (flag) {
                    break;
                }
                retryNum++;
            } catch (Exception e) {
                log.error("Retry failed, message={}", e.getMessage(), e);
                retryNum++;
            }
        }
        return flag;
    }

    @Override
    public long deletePattern(final Object pattern, final Integer count) {
        Set<Object> matchingKeys = scan(String.valueOf(pattern), count);
        return matchingKeys.isEmpty() ? 0L : delete(matchingKeys);
    }

    @Override
    public void delete(final String[] keys) {
        for (String key : keys) {
            delete(key);
        }
    }

    @Override
    public long delete(final Set keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        // Redis Cluster forbids DEL with keys from different hash slots.
        if (RedisConstant.CLUSTER.equalsIgnoreCase(redisMode)) {
            long deleted = 0L;
            for (Object key : keys) {
                if (Boolean.TRUE.equals(redisTemplate.delete(key))) {
                    deleted++;
                }
            }
            return deleted;
        }
        long size = 0;
        Set<Object> deleteKeys = new HashSet<>();
        for (Object key : keys) {
            deleteKeys.add(key);
            if (deleteKeys.size() >= 10) {
                Long count = redisTemplate.delete(deleteKeys);
                if (count == null) {
                    count = 0L;
                }
                size = size + count;
                deleteKeys.clear();
            }
        }
        if (!deleteKeys.isEmpty()) {
            Long count = redisTemplate.delete(deleteKeys);
            if (count == null) {
                count = 0L;
            }
            size = size + count;
            deleteKeys.clear();
        }
        return size;
    }

    @Override
    public boolean vSet(final Object key, Object value) {
        boolean result = false;
        try {
            ValueOperations<Object, Object> operations = redisTemplate.opsForValue();
            operations.set(key, value);
            result = true;
        } catch (Exception e) {
            log.debug(REDIS_OPERATION_FAILED, e);
        }
        return result;
    }

    @Override
    public boolean vSet(final Object key, Object value, Long expireTime) {
        boolean result = false;
        try {
            ValueOperations<Object, Object> operations = redisTemplate.opsForValue();
            if (expireTime == null || expireTime <= 0) {
                throw new IllegalArgumentException("expireTime must be positive seconds");
            }
            operations.set(key, value, expireTime, TimeUnit.SECONDS);
            result = true;
        } catch (Exception e) {
            log.debug(REDIS_OPERATION_FAILED, e);
        }
        return result;
    }

    @Override
    public boolean vSet(Object key, Object value, Duration expire) {
        boolean result = false;
        try {
            redisTemplate.opsForValue().set(key, value, expire);
            result = true;
        } catch (Exception e) {
            log.debug(REDIS_OPERATION_FAILED, e);
        }
        return result;
    }

    @Override
    public boolean vSetUpdate(final Object key, Long expireTime) {
        boolean result = false;
        try {
            redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            result = true;
        } catch (Exception e) {
            log.debug(REDIS_OPERATION_FAILED, e);
        }
        return result;
    }

    @Override
    public Object vGet(final Object key) {
        Object result;
        ValueOperations<Object, Object> operations = redisTemplate.opsForValue();
        result = operations.get(key);
        return result;
    }

    @Override
    public void hmSet(Object key, Object hashKey, Object value) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        hash.put(key, hashKey, value);
    }

    @Override
    public void hmSetAll(Object key, Map<Object, Object> map) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        hash.putAll(key, map);
    }

    @Override
    public Map<Object, Object> hmGet(Object key) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        return hash.entries(key);
    }

    @Override
    public Object hmGet(Object key, Object hashKey) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        return hash.get(key, hashKey);
    }

    @Override
    public Object hmDel(Object key, Object hashKey) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        return hash.delete(key, hashKey);
    }

    @Override
    public long lSize(Object k) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long size = list.size(k);
        if (size == null) {
            return 0L;
        }
        return size;
    }

    @Override
    public Object lRange(Object k) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long size = list.size(k);
        if (size == null) {
            return new ArrayList<>();
        }
        return list.range(k, 0, size);
    }

    @Override
    public List<Object> lRange(Object k, long start, long end) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        return list.range(k, start, end);
    }

    @Override
    public Object lLeftIndexFirst(Object k) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        return list.index(k, 0);
    }

    @Override
    public Object lRightIndexFirst(Object k) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        return list.index(k, -1);
    }

    @Override
    public Object lindex(Object k, long index) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        return list.index(k, index);
    }

    @Override
    public void lLeftPush(Object k, Object v) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        list.leftPush(k, v);
    }

    @Override
    public void lLeftPush(Object k, Object v, boolean bool) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long size = list.size(k);
        if (bool && size != null) {
            list.remove(k, size, v);
        }
        list.leftPush(k, v);
    }

    @Override
    public void lLeftPushAll(Object k, List<Object> lst) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        list.leftPushAll(k, lst);
    }

    @Override
    public void lRightPush(Object k, Object v, boolean bool) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long size = list.size(k);
        if (bool && size != null) {
            list.remove(k, size, v);
        }
        list.rightPush(k, v);
    }

    @Override
    public void lRightPush(Object k, Object v) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        list.rightPush(k, v);
    }

    @Override
    public void lRightPushAll(Object k, List<Object> lst) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        list.rightPushAll(k, lst);
    }

    @Override
    public Object lLeftPop(Object k) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        return list.leftPop(k);
    }

    @Override
    public Object lRightPop(Object k) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        return list.rightPop(k);
    }

    @Override
    public long lRemove(Object k, long count) {
        return removeListElements(k, count, null);
    }

    @Override
    public long lRemove(Object k, long count, @Nullable Object v) {
        if (v == null) {
            return lRemove(k, count);
        }
        return removeListElements(k, count, v);
    }

    /**
     * 通过 {@link RedisCallback} 执行 LREM；{@code null} 值表示匹配 Redis 列表中的 null 元素。
     */
    private long removeListElements(Object k, long count, @Nullable Object value) {
        Long size = redisTemplate.execute((RedisCallback<Long>) connection -> {
            @SuppressWarnings("unchecked")
            byte[] rawKey = ((org.springframework.data.redis.serializer.RedisSerializer<Object>) redisTemplate.getKeySerializer()).serialize(k);
            byte[] rawValue = value == null ? null
                    : ((org.springframework.data.redis.serializer.RedisSerializer<Object>) redisTemplate.getValueSerializer()).serialize(value);
            return connection.listCommands().lRem(rawKey, count, rawValue);
        });
        return size == null ? 0 : size;
    }

    @Override
    public long lRemove(Object k, Object v) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long count = list.size(k);
        if (count != null) {
            Long size = list.remove(k, count, v);
            return size == null ? 0 : size;
        }
        return 0L;
    }

    @Override
    public void sAdd(Object key, Object value) {
        SetOperations<Object, Object> set = redisTemplate.opsForSet();
        set.add(key, value);
    }

    @Override
    public Set<Object> sMembers(Object key) {
        SetOperations<Object, Object> set = redisTemplate.opsForSet();
        return set.members(key);
    }

    /**
     * 随机获取变量中的元素
     *
     * @param key 键
     * @return
     */
    @Override
    public Object sRandomMember(String key) {
        return redisTemplate.opsForSet().randomMember(key);
    }

    @Override
    public Object sPop(String key) {
        return redisTemplate.opsForSet().pop(key);
    }

    @Override
    public void sRemove(String key, Object... values) {
        redisTemplate.opsForSet().remove(key, values);
    }

    @Override
    public void zAdd(Object key, Object value, double scoure) {
        ZSetOperations<Object, Object> zset = redisTemplate.opsForZSet();
        zset.add(key, value, scoure);
    }

    @Override
    public Set<Object> rangeByScore(Object key, double scoure, double scoure1) {
        ZSetOperations<Object, Object> zset = redisTemplate.opsForZSet();
        return zset.rangeByScore(key, scoure, scoure1);
    }

    @Override
    public void hmSetIncrement(Object key, Object hashKey, Long value) {
        HashOperations<Object, Object, Object> hash = redisTemplate.opsForHash();
        hash.increment(key, hashKey, value);
    }

    /**
     * 通过 Redis SCAN 获取匹配 key。count 只是每轮 SCAN 的提示值，
     * 不是结果数量上限；该方法可能累积大量 key，禁止在热路径使用。
     *
     * <p>Cluster 模式对全部 master 分别 SCAN；连接和 Cursor 均由本方法关闭。
     * Redis SCAN 并非并发写入下的一致快照，调用方需容忍重复与变化。</p>
     *
     * @param matchKey Redis glob pattern
     * @param count 每轮扫描提示值，空或非正值使用 1000
     * @return 匹配 key 的去重集合，空集合表示没有匹配
     */
    @Override
    public Set<Object> scan(String matchKey, Integer count) {
        if (StringUtils.isBlank(matchKey)) {
            throw new IllegalArgumentException("Redis SCAN pattern cannot be blank");
        }
        long batchSize = count == null || count <= 0 ? 1000L : count.longValue();
        ScanOptions options = ScanOptions.scanOptions().match(matchKey).count(batchSize).build();
        Set<Object> matchingKeys = new HashSet<>();

        try {
            if (RedisConstant.CLUSTER.equalsIgnoreCase(redisMode)) {
                var connectionFactory = Objects.requireNonNull(redisTemplate.getConnectionFactory(),
                        "RedisConnectionFactory must not be null");
                try (RedisClusterConnection connection = connectionFactory.getClusterConnection()) {
                    for (RedisClusterNode node : connection.clusterGetNodes()) {
                        if (!node.isMaster()) {
                            continue;
                        }
                        try (Cursor<byte[]> cursor = connection.scan(node, options)) {
                            while (cursor.hasNext()) {
                                Object key = redisTemplate.getKeySerializer().deserialize(cursor.next());
                                if (key != null) {
                                    matchingKeys.add(key);
                                }
                            }
                        }
                    }
                }
            } else {
                try (Cursor<Object> cursor = redisTemplate.scan(options)) {
                    while (cursor.hasNext()) {
                        matchingKeys.add(cursor.next());
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Redis SCAN failed, pattern=" + matchKey, e);
        }
        return matchingKeys;
    }

    @Override
    public Map hscan(String matchKey, Integer count) {
        Map<Object, Object> map = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match(matchKey).count(count).build();
        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan("field", options)) {
            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();
                map.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Redis HSCAN failed, matchKey=" + matchKey, e);
        }
        return map;
    }

    @Override
    public Set<String> sscan(String matchKey, Integer count) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(matchKey).count(count).build();
        try (Cursor<Object> cursor = redisTemplate.opsForSet().scan("setValue", options)) {
            while (cursor.hasNext()) {
                keys.add(String.valueOf(cursor.next()));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Redis SSCAN failed, matchKey=" + matchKey, e);
        }
        return keys;
    }

    @Override
    public Map zscan(String matchKey, Integer count) {
        Map<Object, Object> map = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match(matchKey).count(count).build();
        try (Cursor<ZSetOperations.TypedTuple<Object>> cursor = redisTemplate.opsForZSet().scan("zSetValue", options)) {
            while (cursor.hasNext()) {
                ZSetOperations.TypedTuple<Object> tuple = cursor.next();
                map.put(tuple.getValue(), tuple.getScore());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Redis ZSCAN failed, matchKey=" + matchKey, e);
        }
        return map;
    }

    @Override
    public Set<Object> keys(String matchKey) {
        return scan(matchKey, null);
    }

    /**
     * 向通道发送消息的方法
     *
     * @param channel
     * @param message
     */
    @Override
    public void convertAndSend(String channel, String message) {
        try {
            redisTemplate.convertAndSend(channel, message);
        } catch (Exception e) {
            log.debug(REDIS_OPERATION_FAILED, e);
        }
    }

    /**
     * 向通道发送消息的方法
     *
     * @param channel
     * @param message
     */
    @Override
    public void convertAndSend(String channel, Object message) {
        try {
            redisTemplate.convertAndSend(channel, message);
        } catch (Exception e) {
            log.debug(REDIS_OPERATION_FAILED, e);
        }
    }

    @Override
    public void expire(Object k, long timeout) {
        try {
            redisTemplate.expire(k, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug(REDIS_OPERATION_FAILED, e);
        }
    }

    @Override
    public long increaseNum(String key, long fromIndex) {
        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex不能小于0");
        }
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("key不能为空");
        }
        Long increment = redisTemplate.opsForValue().increment(key, fromIndex);
        return increment == null ? 0 : increment;
    }

    @Override
    public long decreaseNum(String key, long decIndex) {
        if (decIndex < 0) {
            throw new IllegalArgumentException("decIndex不能小于0");
        }
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("key不能为空");
        }
        Long decrement = redisTemplate.opsForValue().decrement(key, decIndex);
        return decrement == null ? 0 : decrement;
    }
}
