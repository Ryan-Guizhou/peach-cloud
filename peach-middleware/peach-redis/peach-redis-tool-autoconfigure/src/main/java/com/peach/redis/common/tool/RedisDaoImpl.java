

package com.peach.redis.common.tool;

import jakarta.annotation.Nullable;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.ClusterTopology;
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

    private static final String REDIS_CLUSTER_MODE = "cluster";

    private static final int DEFAULT_SCAN_MAX_RESULTS = 10_000;

    private static final long DEFAULT_SCAN_TIMEOUT_MILLIS = 10_000L;

    private static final int SCAN_BATCH_MAX_HINT = 1_000;

    @Value("${peach.redis.mode:standalone}")
    private String redisMode;

    @Value("${peach.redis.tool.scan.max-results:10000}")
    private int scanMaxResults = DEFAULT_SCAN_MAX_RESULTS;

    @Value("${peach.redis.tool.scan.timeout-millis:10000}")
    private long scanTimeoutMillis = DEFAULT_SCAN_TIMEOUT_MILLIS;

    public RedisDaoImpl(RedisTemplate<?, ?> redisTemplate) {
        super(toObjectTemplate(redisTemplate));
    }

    /**
     * 将统一 RedisTemplate 适配到历史 RedisDao 的 Object key/value 契约。
     *
     * <p>RedisTemplate 的泛型只参与编译期类型检查，实际 key 序列化仍由
     * 当前应用注入的 {@code RedisTemplate} 配置为字符串键序列化器。</p>
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
        if (pattern == null) {
            throw new IllegalArgumentException("Redis delete pattern cannot be null");
        }
        String glob = String.valueOf(pattern);
        validateDeletePattern(glob);
        // 先完成有界扫描；超限时不得开始删除已经找到的那部分键。
        Set<Object> matchingKeys = scan(glob, count);
        return matchingKeys.isEmpty() ? 0L : delete(matchingKeys);
    }

    /**
     * 拒绝无业务命名空间的通配删除；精确键可继续使用。
     * 不允许把客户端任意 pattern 直接拼入全局删库操作。
     */
    private static void validateDeletePattern(String pattern) {
        if (StringUtils.isBlank(pattern) || pattern.length() > 512) {
            throw new IllegalArgumentException("Redis delete pattern length is invalid");
        }
        int wildcard = pattern.length();
        for (char special : new char[]{'*', '?', '['}) {
            int index = pattern.indexOf(special);
            if (index >= 0 && index < wildcard) {
                wildcard = index;
            }
        }
        if (wildcard < pattern.length()) {
            String namespace = pattern.substring(0, wildcard);
            if (namespace.length() < 4 || !namespace.contains(":")) {
                throw new IllegalArgumentException("Redis wildcard deletion requires a fixed business namespace");
            }
        }
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
        if (REDIS_CLUSTER_MODE.equalsIgnoreCase(redisMode)) {
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
            result = Boolean.TRUE.equals(redisTemplate.expire(key, expireTime, TimeUnit.SECONDS));
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
     * 通过 Redis SCAN 有界遍历匹配键。count 只控制单次游标提示值，
     * 最多返回 scanMaxResults 个不同的键，超出容量或时间预算明确失败，
     * 不返回被截断的结果集，也不在本方法中执行删除。
     *
     * <p>Cluster 模式扫描全部活动 master，Cursor 与连接始终关闭。
     * Redis SCAN 不是一致快照，并发写入可能导致遗漏或重复。</p>
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
        validateScanBounds();
        long batchSize = count == null || count <= 0
                ? SCAN_BATCH_MAX_HINT : Math.min(count.longValue(), SCAN_BATCH_MAX_HINT);
        ScanOptions options = ScanOptions.scanOptions().match(matchKey).count(batchSize).build();
        Set<Object> matchingKeys = new HashSet<>();
        long startedNanos = System.nanoTime();
        long maxElapsedNanos = TimeUnit.MILLISECONDS.toNanos(scanTimeoutMillis);

        try {
            if (REDIS_CLUSTER_MODE.equalsIgnoreCase(redisMode)) {
                var connectionFactory = Objects.requireNonNull(redisTemplate.getConnectionFactory(),
                        "RedisConnectionFactory must not be null");
                try (RedisClusterConnection connection = connectionFactory.getClusterConnection()) {
                    // The topology may still list a dead former master after replica promotion.
                    // Spring Data's active-master filter ignores disconnected/FAIL/PFAIL nodes.
                    Set<RedisClusterNode> reportedNodes = new HashSet<>();
                    connection.clusterGetNodes().forEach(reportedNodes::add);
                    Set<RedisClusterNode> activeMasters =
                            new ClusterTopology(reportedNodes).getActiveMasterNodes();
                    if (activeMasters.isEmpty()) {
                        throw new IllegalStateException("No active Redis Cluster masters available for SCAN");
                    }
                    // A partial topology must fail instead of silently returning incomplete keys.
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
                        assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
                        try (Cursor<byte[]> cursor = connection.scan(node, options)) {
                            while (true) {
                                assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
                                if (!cursor.hasNext()) {
                                    break;
                                }
                                Object key = redisTemplate.getKeySerializer().deserialize(cursor.next());
                                acceptScannedKey(matchingKeys, key, startedNanos, maxElapsedNanos);
                            }
                        }
                    }
                }
            } else {
                try (Cursor<Object> cursor = redisTemplate.scan(options)) {
                    while (true) {
                        assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
                        if (!cursor.hasNext()) {
                            break;
                        }
                        acceptScannedKey(matchingKeys, cursor.next(), startedNanos, maxElapsedNanos);
                    }
                }
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            // 原因链仍可用于排查，错误正文不暴露 Redis 业务 key。
            throw new IllegalStateException("Redis SCAN failed", exception);
        }
        // 无匹配 key 时同样需要检查遍历耗时，不能只在收集结果时校验。
        assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
        return matchingKeys;
    }

    private void validateScanBounds() {
        if (scanMaxResults < 1 || scanMaxResults > 100_000
                || scanTimeoutMillis < 1L || scanTimeoutMillis > 60_000L) {
            throw new IllegalArgumentException("Redis SCAN limits are invalid");
        }
    }

    /**
     * 集中执行数量/耗时防护；不以 COUNT 作为结果容量限制。
     */
    private void acceptScannedKey(Set<Object> matchingKeys, Object key,
                                  long startedNanos, long maxElapsedNanos) {
        assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
        if (key != null && matchingKeys.add(key) && matchingKeys.size() > scanMaxResults) {
            throw new IllegalStateException("Redis SCAN exceeded configured result limit");
        }
    }

    private static void assertScanWithinTimeBudget(long startedNanos, long maxElapsedNanos) {
        if (System.nanoTime() - startedNanos > maxElapsedNanos) {
            throw new IllegalStateException("Redis SCAN exceeded configured time budget");
        }
    }

    /**
     * 遍历指定 Hash 键的全部字段；count 只表示单次游标提示，不是成员上限。
     * 复用全局扫描数量和耗时限制，避免无限收集。
     */
    @Override
    public Map hscan(String matchKey, Integer count) {
        validateCollectionKey(matchKey);
        validateScanBounds();
        Map<Object, Object> values = new HashMap<>();
        long startedNanos = System.nanoTime();
        long maxElapsedNanos = TimeUnit.MILLISECONDS.toNanos(scanTimeoutMillis);
        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash()
                .scan(matchKey, collectionScanOptions(count))) {
            while (true) {
                assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
                if (!cursor.hasNext()) {
                    break;
                }
                Map.Entry<Object, Object> entry = cursor.next();
                values.put(entry.getKey(), entry.getValue());
                if (values.size() > scanMaxResults) {
                    throw new IllegalStateException("Redis HSCAN exceeded configured result limit");
                }
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Redis HSCAN failed", exception);
        }
        assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
        return values;
    }

    /**
     * 遍历指定 Set 键的成员；超限时明确抛错。
     */
    @Override
    public Set<String> sscan(String matchKey, Integer count) {
        validateCollectionKey(matchKey);
        validateScanBounds();
        Set<String> values = new HashSet<>();
        long startedNanos = System.nanoTime();
        long maxElapsedNanos = TimeUnit.MILLISECONDS.toNanos(scanTimeoutMillis);
        try (Cursor<Object> cursor = redisTemplate.opsForSet()
                .scan(matchKey, collectionScanOptions(count))) {
            while (true) {
                assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
                if (!cursor.hasNext()) {
                    break;
                }
                values.add(String.valueOf(cursor.next()));
                if (values.size() > scanMaxResults) {
                    throw new IllegalStateException("Redis SSCAN exceeded configured result limit");
                }
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Redis SSCAN failed", exception);
        }
        assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
        return values;
    }

    /**
     * 遍历指定 ZSet 键的成员和分值；超限时明确抛错。
     */
    @Override
    public Map zscan(String matchKey, Integer count) {
        validateCollectionKey(matchKey);
        validateScanBounds();
        Map<Object, Object> values = new HashMap<>();
        long startedNanos = System.nanoTime();
        long maxElapsedNanos = TimeUnit.MILLISECONDS.toNanos(scanTimeoutMillis);
        try (Cursor<ZSetOperations.TypedTuple<Object>> cursor = redisTemplate.opsForZSet()
                .scan(matchKey, collectionScanOptions(count))) {
            while (true) {
                assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
                if (!cursor.hasNext()) {
                    break;
                }
                ZSetOperations.TypedTuple<Object> entry = cursor.next();
                values.put(entry.getValue(), entry.getScore());
                if (values.size() > scanMaxResults) {
                    throw new IllegalStateException("Redis ZSCAN exceeded configured result limit");
                }
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Redis ZSCAN failed", exception);
        }
        assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
        return values;
    }

    private static void validateCollectionKey(String key) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("Redis collection key cannot be blank");
        }
    }

    private static ScanOptions collectionScanOptions(Integer count) {
        long hint = count == null || count <= 0 ? SCAN_BATCH_MAX_HINT
                : Math.min(count.longValue(), SCAN_BATCH_MAX_HINT);
        return ScanOptions.scanOptions().count(hint).build();
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
