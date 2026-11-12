package com.peach.redis.tool;

import com.peach.redis.tool.autoconfigure.RedisToolProperties;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.ClusterTopology;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.stereotype.Indexed;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 基于应用统一 RedisTemplate 的同步数据访问实现。
 *
 * <p>配置在构造时读取，实例不持有请求级业务状态；连接工厂与模板由 Spring 管理。
 * 扫描方法在本地限制结果数量及遍历时间，并关闭自行取得的游标和 Cluster 连接。
 * 多键删除在 Cluster 中逐键执行，支持跨槽但不提供整体事务。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:39
 */
@Slf4j
@Indexed
public class RedisDaoImpl extends AbstractBaseRedisDao<Object, Object> implements RedisDao {

    private static final String REDIS_CLUSTER_MODE = "cluster";

    private static final int DEFAULT_SCAN_MAX_RESULTS = 10_000;

    private static final long DEFAULT_SCAN_TIMEOUT_MILLIS = 10_000L;

    private static final int SCAN_BATCH_MAX_HINT = 1_000;

    private final String redisMode;

    private int scanMaxResults = DEFAULT_SCAN_MAX_RESULTS;

    private long scanTimeoutMillis = DEFAULT_SCAN_TIMEOUT_MILLIS;

    /**
     * 使用默认扫描预算和 standalone 模式创建 DAO，不接管模板生命周期。
     *
     * @param redisTemplate 应用统一模板；集群调用方应使用显式配置构造器
     */
    public RedisDaoImpl(RedisTemplate<?, ?> redisTemplate) {
        this(redisTemplate, new RedisToolProperties(), "standalone");
    }

    public RedisDaoImpl(RedisTemplate<?, ?> redisTemplate, RedisToolProperties properties, String mode) {
        super(toObjectTemplate(redisTemplate));
        Objects.requireNonNull(properties, "properties");
        this.redisMode = Objects.requireNonNull(mode, "mode");
        this.scanMaxResults = properties.getScan().getMaxResults();
        this.scanTimeoutMillis = properties.getScan().getTimeoutMillis();
        validateScanBounds();
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
            throw new IllegalStateException("Redis EXISTS returned no confirmation");
        }
        return retryFlag;
    }

    @Override
    public boolean delete(final Object key) {
        Objects.requireNonNull(key, "key");
        Boolean deleted = redisTemplate.delete(key);
        if (deleted == null) {
            throw new IllegalStateException("Redis DEL returned no confirmation");
        }
        return deleted;
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

    @Override
    public long delete(final Set keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        // Cluster 不允许跨槽 DEL，多键清理按精确键逐一执行。
        if (REDIS_CLUSTER_MODE.equalsIgnoreCase(redisMode)) {
            long deleted = 0L;
            for (Object key : keys) {
                if (delete(key)) {
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
                    throw new IllegalStateException("Redis batch DEL returned no confirmation");
                }
                size = Math.addExact(size, count);
                deleteKeys.clear();
            }
        }
        if (!deleteKeys.isEmpty()) {
            Long count = redisTemplate.delete(deleteKeys);
            if (count == null) {
                throw new IllegalStateException("Redis batch DEL returned no confirmation");
            }
            size = Math.addExact(size, count);
            deleteKeys.clear();
        }
        return size;
    }

    @Override
    public boolean vSet(final Object key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        return true;
    }

    @Override
    public boolean vSet(final Object key, Object value, Long expireTime) {
        if (expireTime == null || expireTime <= 0L) {
            throw new IllegalArgumentException("expireTime must be positive seconds");
        }
        redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean vSet(Object key, Object value, Duration expire) {
        if (expire == null || expire.isNegative() || expire.isZero()) {
            throw new IllegalArgumentException("Redis value expiration must be positive");
        }
        redisTemplate.opsForValue().set(key, value, expire);
        return true;
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
    public long lSize(Object k) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long size = list.size(k);
        if (size == null) {
            return 0L;
        }
        return size;
    }

    @Override
    public List<Object> lRange(Object k, long start, long end) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        return list.range(k, start, end);
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
    public void lLeftPushAll(Object k, List<Object> lst) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        list.leftPushAll(k, lst);
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
    public long lRemove(Object k, long count, Object v) {
        return removeListElements(k, count, Objects.requireNonNull(v, "value"));
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
                    // 故障切换后拓扑可能仍包含旧主节点，只扫描连接正常且未被标记故障的活动主节点。
                    Set<RedisClusterNode> reportedNodes = new HashSet<>();
                    connection.clusterGetNodes().forEach(reportedNodes::add);
                    Set<RedisClusterNode> activeMasters =
                            new ClusterTopology(reportedNodes).getActiveMasterNodes();
                    if (activeMasters.isEmpty()) {
                        throw new IllegalStateException("No active Redis Cluster masters available for SCAN");
                    }
                    // 槽位覆盖不完整时明确失败，避免返回缺失节点的扫描结果。
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

    @Override
    public void convertAndSend(String channel, Object message) {
        validateChannel(channel);
        Long subscribers = redisTemplate.convertAndSend(channel, message);
        if (subscribers == null) {
            throw new IllegalStateException("Redis PUBLISH returned no confirmation");
        }
    }

    @Override
    public long increaseNum(String key, long fromIndex) {
        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex must not be negative");
        }
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("key must not be blank");
        }
        Long increment = redisTemplate.opsForValue().increment(key, fromIndex);
        if (increment == null) {
            throw new IllegalStateException("Redis INCRBY returned no confirmation");
        }
        return increment;
    }


    @Override
    public long decreaseNum(String key, long decIndex) {
        if (decIndex < 0) {
            throw new IllegalArgumentException("decIndex must not be negative");
        }
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("key must not be blank");
        }
        Long decrement = redisTemplate.opsForValue().decrement(key, decIndex);
        if (decrement == null) {
            throw new IllegalStateException("Redis DECRBY returned no confirmation");
        }
        return decrement;
    }

    @Override
    public long ttl(String key) {
        return confirmed(redisTemplate.getExpire(key, TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean expire(String key, Duration timeout) {
        return confirmed(redisTemplate.expire(key, positive(timeout)));
    }


    @Override
    public boolean persist(String key) {
        return confirmed(redisTemplate.persist(key));
    }

    @Override
    public List<Object> vMultiGet(List<String> keys) {
        Objects.requireNonNull(keys, "keys");
        List<Object> results = new ArrayList<>(keys.size());
        for (String key : keys) {
            validateCollectionKey(key);
            results.add(vGet(key));
        }
        return results;
    }

    @Override
    public boolean vSetIfAbsent(String key, Object value, Duration ttl) {
        return confirmed(redisTemplate.opsForValue().setIfAbsent(key, value, positive(ttl)));
    }

    @Override
    public boolean vSetIfPresent(String key, Object value, Duration ttl) {
        return confirmed(redisTemplate.opsForValue().setIfPresent(key, value, positive(ttl)));
    }

    @Override
    public Object vGetAndDelete(String key) {
        return redisTemplate.opsForValue().getAndDelete(key);
    }

    @Override
    public boolean hmHasKey(String key, String field) {
        return confirmed(redisTemplate.opsForHash().hasKey(key, field));
    }

    @Override
    public long hmSize(String key) {
        return confirmed(redisTemplate.opsForHash().size(key));
    }

    @Override
    public List<Object> hmMultiGet(String key, List<String> fields) {
        return confirmed(redisTemplate.opsForHash().multiGet(key, new ArrayList<Object>(fields)));
    }

    @Override
    public long hmDelete(String key, String... fields) {
        return fields.length == 0 ? 0L : confirmed(redisTemplate.opsForHash().delete(key, (Object[]) fields));
    }

    @Override
    public long hmIncrement(String key, String field, long delta) {
        return confirmed(redisTemplate.opsForHash().increment(key, field, delta));
    }

    @Override
    public double hmIncrement(String key, String field, double delta) {
        finite(delta);
        return confirmed(redisTemplate.opsForHash().increment(key, field, delta));
    }

    @Override
    public void lSet(String key, long index, Object value) {
        redisTemplate.opsForList().set(key, index, value);
    }

    @Override
    public void lTrim(String key, long start, long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    @Override
    public long sAddAll(String key, Object... values) {
        return values.length == 0 ? 0L : confirmed(redisTemplate.opsForSet().add(key, values));
    }

    @Override
    public boolean sIsMember(String key, Object value) {
        return confirmed(redisTemplate.opsForSet().isMember(key, value));
    }

    @Override
    public long sSize(String key) {
        return confirmed(redisTemplate.opsForSet().size(key));
    }

    @Override
    public Set<Object> sIntersect(String key, String otherKey) {
        Set<Object> result = new HashSet<>(sScan(key, null));
        result.retainAll(sScan(otherKey, null));
        return result;
    }

    @Override
    public Set<Object> sUnion(String key, String otherKey) {
        Set<Object> result = new HashSet<>(sScan(key, null));
        result.addAll(sScan(otherKey, null));
        return result;
    }

    @Override
    public Set<Object> sDifference(String key, String otherKey) {
        Set<Object> result = new HashSet<>(sScan(key, null));
        result.removeAll(sScan(otherKey, null));
        return result;
    }

    @Override
    public long zAddAll(String key, Map<Object, Double> scores) {
        Set<ZSetOperations.TypedTuple<Object>> tuples = new HashSet<>();
        scores.forEach((value, score) -> {
            finite(score);
            tuples.add(new org.springframework.data.redis.core.DefaultTypedTuple<>(Objects.requireNonNull(value), score));
        });
        return tuples.isEmpty() ? 0L : confirmed(redisTemplate.opsForZSet().add(key, tuples));
    }

    @Override
    public long zRemove(String key, Object... values) {
        return values.length == 0 ? 0L : confirmed(redisTemplate.opsForZSet().remove(key, values));
    }

    @Override
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    @Override
    public double zIncrementScore(String key, Object value, double delta) {
        finite(delta);
        return confirmed(redisTemplate.opsForZSet().incrementScore(key, value, delta));
    }

    @Override
    public Long zRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    @Override
    public Long zReverseRank(String key, Object value) {
        return redisTemplate.opsForZSet().reverseRank(key, value);
    }

    @Override
    public Set<Object> zRange(String key, long start, long end) {
        return confirmed(redisTemplate.opsForZSet().range(key, start, end));
    }

    @Override
    public Set<Object> zReverseRange(String key, long start, long end) {
        return confirmed(redisTemplate.opsForZSet().reverseRange(key, start, end));
    }

    @Override
    public Set<ZSetOperations.TypedTuple<Object>> zRangeWithScores(String key, long start, long end) {
        return confirmed(redisTemplate.opsForZSet().rangeWithScores(key, start, end));
    }

    @Override
    public Set<Object> zRangeByScore(String key, double min, double max, long offset, long count) {
        page(offset, count);
        scoreRange(min, max);
        return confirmed(redisTemplate.opsForZSet().rangeByScore(key, min, max, offset, count));
    }

    @Override
    public Set<Object> zReverseRangeByScore(String key, double min, double max, long offset, long count) {
        page(offset, count);
        scoreRange(min, max);
        return confirmed(redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, offset, count));
    }

    @Override
    public long zSize(String key) {
        return confirmed(redisTemplate.opsForZSet().size(key));
    }

    @Override
    public long zCount(String key, double min, double max) {
        scoreRange(min, max);
        return confirmed(redisTemplate.opsForZSet().count(key, min, max));
    }

    @Override
    public long zRemoveRange(String key, long start, long end) {
        return confirmed(redisTemplate.opsForZSet().removeRange(key, start, end));
    }

    @Override
    public long zRemoveRangeByScore(String key, double min, double max) {
        scoreRange(min, max);
        return confirmed(redisTemplate.opsForZSet().removeRangeByScore(key, min, max));
    }

    @Override
    public Map<Object, Object> hmScan(String key, Integer count) {
        return collectScan(key, () -> redisTemplate.opsForHash().scan(key, collectionScanOptions(count)),
                Map.Entry::getKey, Map.Entry::getValue);
    }

    @Override
    public Set<Object> sScan(String key, Integer count) {
        return collectScan(key, () -> redisTemplate.opsForSet().scan(key, collectionScanOptions(count)),
                value -> value, value -> Boolean.TRUE).keySet();
    }


    @Override
    public Map<Object, Double> zScan(String key, Integer count) {
        return collectScan(key, () -> redisTemplate.opsForZSet().scan(key, collectionScanOptions(count)),
                ZSetOperations.TypedTuple::getValue, ZSetOperations.TypedTuple::getScore);
    }

    /**
     * 在同一数量和时间预算内收集集合游标，内部负责关闭游标。
     *
     * <p>先校验实际键再打开游标；按映射后的成员去重，超限直接抛异常，
     * 不暴露已收集的部分结果。游标关闭失败同样向上传播。</p>
     *
     * @param key 实际集合键
     * @param supplier 创建本次独占游标的工厂
     * @param keyMapper 成员标识转换函数
     * @param valueMapper 成员值或分值转换函数
     * @param <T> 游标元素类型
     * @param <V> 收集值类型
     * @return 完整且未超限的成员映射
     */
    private <T, V> Map<Object, V> collectScan(String key, Supplier<Cursor<T>> supplier,
                                             Function<T, Object> keyMapper,
                                             Function<T, V> valueMapper) {
        validateCollectionKey(key);
        validateScanBounds();
        long started = System.nanoTime();
        long budget = TimeUnit.MILLISECONDS.toNanos(scanTimeoutMillis);
        Map<Object, V> result = new HashMap<>();
        try (Cursor<T> cursor = supplier.get()) {
            while (true) {
                assertScanWithinTimeBudget(started, budget);
                if (!cursor.hasNext()) {
                    break;
                }
                T item = cursor.next();
                result.put(keyMapper.apply(item), valueMapper.apply(item));
                if (result.size() > scanMaxResults) {
                    throw new IllegalStateException("Redis collection SCAN exceeded configured result limit");
                }
            }
        }
        assertScanWithinTimeBudget(started, budget);
        return result;
    }

    /**
     * 排除 Pipeline、事务排队等无同步确认的结果，不把未知结果解释为成功。
     *
     * <p>Redisson / RedisTemplate 在 pipeline、事务等异步场景下可能返回 {@code null}，
     * 此时命令是否真正执行无法确定，必须显式失败，避免上层把“无确认”误判为成功。</p>
     *
     * @param value 命令返回值，允许为 {@code null}
     * @param <T>   返回值类型
     * @return 非空的命令返回值
     * @throws IllegalStateException 当 {@code value} 为 {@code null} 时抛出
     */
    private static <T> T confirmed(T value) {
        if (value == null) {
            throw new IllegalStateException("Redis command returned no confirmation; pipeline/transaction usage is unsupported");
        }
        return value;
    }

    /**
     * 新增 TTL 接口使用毫秒精度，拒绝转换后不足 1 毫秒的正时长。
     *
     * <p>Redis 的过期时间以毫秒为最小单位，小于 1ms 的 TTL 在转换后会被截断为 0，
     * 导致 key 立即过期或行为不可预期，因此在此处统一拒绝。</p>
     *
     * @param ttl 过期时长，允许为 {@code null}
     * @return 校验通过的原始 {@link Duration}
     * @throws IllegalArgumentException 当 {@code ttl} 为 {@code null}、负数、零或不足 1 毫秒时抛出
     */
    private static Duration positive(Duration ttl) {
        if (ttl == null || ttl.isNegative() || ttl.isZero() || ttl.toMillis() < 1) {
            throw new IllegalArgumentException("Redis expiration must be at least one millisecond");
        }
        return ttl;
    }

    /**
     * 数值写入拒绝 NaN 和无穷，查询区间的无穷端点由 scoreRange 单独处理。
     *
     * <p>写入类操作（如 ZADD 的 score）必须使用有限数值；查询区间的开闭端允许
     * {@code +/-Infinity}，因此不在本方法中拦截，由 {@link #scoreRange(double, double)} 负责。</p>
     *
     * @param number 待写入的数值
     * @throws IllegalArgumentException 当 {@code number} 为 NaN 或无穷时抛出
     */
    private static void finite(double number) {
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Redis numeric value must be finite");
        }
    }

    /**
     * 查询区间允许无穷端点，但不允许 NaN 或下界大于上界。
     *
     * <p>与 {@link #finite(double)} 的区别：区间查询允许 {@code -Infinity} / {@code +Infinity}
     * 作为端点，表示无界区间；但 NaN 和 {@code min > max} 属于非法区间，必须拒绝。</p>
     *
     * @param min 区间下界，允许为无穷
     * @param max 区间上界，允许为无穷
     * @throws IllegalArgumentException 当任一参数为 NaN 或 {@code min > max} 时抛出
     */
    private static void scoreRange(double min, double max) {
        if (Double.isNaN(min) || Double.isNaN(max) || min > max) {
            throw new IllegalArgumentException("Invalid Redis score range");
        }
    }

    /**
     * 分值分页必须显式限定正数页大小，不将非正数解释为读取全部。
     *
     * <p>部分 Redis 命令对 {@code count <= 0} 有特殊语义（如读取全部），
     * 为避免调用方误传导致意外全量读取，此处强制要求 offset 非负、count 为正。</p>
     *
     * @param offset 偏移量，必须大于等于 0
     * @param count  单页数量，必须大于等于 1
     * @throws IllegalArgumentException 当 {@code offset < 0} 或 {@code count < 1} 时抛出
     */
    private static void page(long offset, long count) {
        if (offset < 0 || count < 1) {
            throw new IllegalArgumentException("Redis page offset must be nonnegative and count positive");
        }
    }

    /**
     * 拒绝无业务命名空间的通配删除；精确键可继续使用。
     * 不允许把客户端任意 pattern 直接拼入全局删库操作。
     *
     * <p>校验规则：</p>
     * <ul>
     *     <li>pattern 不能为空白，长度不超过 512；</li>
     *     <li>若包含通配符 {@code *}、{@code ?}、{@code [}，则通配符之前的前缀必须
     *         至少 4 个字符且包含 {@code :}，作为固定业务命名空间；</li>
     *     <li>不含通配符的精确键不做命名空间限制，可直接使用。</li>
     * </ul>
     *
     * @param pattern 待删除的 key 或 pattern
     * @throws IllegalArgumentException 当 pattern 为空、过长或通配删除缺少合法业务命名空间时抛出
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

    /**
     * 执行显式值匹配的 LREM；COUNT=0 表示全部，负数从尾部开始匹配。
     *
     * <p>对应 Redis {@code LREM key count value} 语义：</p>
     * <ul>
     *     <li>{@code count > 0}：从头部开始删除最多 count 个匹配元素；</li>
     *     <li>{@code count = 0}：删除所有匹配元素；</li>
     *     <li>{@code count < 0}：从尾部开始删除最多 |count| 个匹配元素。</li>
     * </ul>
     *
     * @param key   List 的 key
     * @param count 删除数量与方向，语义见上文
     * @param value 待匹配并删除的元素值
     * @return 实际删除的元素数量
     * @throws IllegalStateException 当底层返回 {@code null}（无同步确认）时抛出
     */
    private long removeListElements(Object key, long count, Object value) {
        Long removed = redisTemplate.opsForList().remove(key, count, value);
        if (removed == null) {
            throw new IllegalStateException("Redis LREM returned no confirmation");
        }
        return removed;
    }

    /**
     * 校验预算有效范围，既用于启动校验，也防止扫描时使用非法预算。
     *
     * <p>约束：{@code scanMaxResults} 取值 [1, 100000]，{@code scanTimeoutMillis} 取值 [1, 60000]。
     * 启动阶段校验可尽早暴露错误配置，运行期再次校验可防止动态修改后越界。</p>
     *
     * @throws IllegalArgumentException 当扫描结果上限或超时预算超出允许范围时抛出
     */
    private void validateScanBounds() {
        if (scanMaxResults < 1 || scanMaxResults > 100_000
                || scanTimeoutMillis < 1L || scanTimeoutMillis > 60_000L) {
            throw new IllegalArgumentException("Redis SCAN limits are invalid");
        }
    }

    /**
     * 集中执行数量/耗时防护；不以 COUNT 作为结果容量限制。
     *
     * <p>{@code SCAN} 的 {@code COUNT} 只是单轮游标的提示值，不保证返回数量，
     * 因此结果容量必须在客户端累计校验。本方法在接收每个 key 时同时检查：</p>
     * <ul>
     *     <li>整次扫描是否超出时间预算；</li>
     *     <li>去重后的结果数量是否超出 {@code scanMaxResults}。</li>
     * </ul>
     *
     * @param matchingKeys    已收集的匹配 key 集合，用于去重和计数
     * @param key             本轮扫描返回的 key，允许为 {@code null}（会被忽略）
     * @param startedNanos    扫描开始时的单调时钟纳秒值
     * @param maxElapsedNanos 允许的最大耗时纳秒值
     * @throws IllegalStateException 当超出时间预算或结果数量上限时抛出
     */
    private void acceptScannedKey(Set<Object> matchingKeys, Object key,
                                  long startedNanos, long maxElapsedNanos) {
        assertScanWithinTimeBudget(startedNanos, maxElapsedNanos);
        if (key != null && matchingKeys.add(key) && matchingKeys.size() > scanMaxResults) {
            throw new IllegalStateException("Redis SCAN exceeded configured result limit");
        }
    }

    /**
     * 使用单调时钟检查整次遍历耗时，不能中断当前正在阻塞的网络调用。
     *
     * <p>说明：本方法只能在两次网络调用之间检查耗时，无法中断已经阻塞的
     * Redis 请求；若需要硬性中断，需依赖客户端超时配置。</p>
     *
     * @param startedNanos    扫描开始时的 {@link System#nanoTime()} 值
     * @param maxElapsedNanos 允许的最大耗时纳秒值
     * @throws IllegalStateException 当已耗时超过预算时抛出
     */
    private static void assertScanWithinTimeBudget(long startedNanos, long maxElapsedNanos) {
        if (System.nanoTime() - startedNanos > maxElapsedNanos) {
            throw new IllegalStateException("Redis SCAN exceeded configured time budget");
        }
    }

    /**
     * 集合扫描接收实际键，拒绝将空白字符串误作扫描目标。
     *
     * @param key 集合类 key
     * @throws IllegalArgumentException 当 {@code key} 为空白时抛出
     */
    private static void validateCollectionKey(String key) {
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("Redis collection key cannot be blank");
        }
    }

    /**
     * 限制单轮游标提示值；该值不替代整次扫描的结果数量上限。
     *
     * <p>{@code SCAN} 的 {@code COUNT} 仅作为单轮返回量的提示，不是硬上限。
     * 本方法对调用方传入的 count 做上限裁剪：为 {@code null} 或非正数时使用默认值
     * {@code SCAN_BATCH_MAX_HINT}，超过上限时截断为上限值。</p>
     *
     * @param count 调用方期望的单轮提示数量，允许为 {@code null}
     * @return 构建好的 {@link ScanOptions}
     */
    private static ScanOptions collectionScanOptions(Integer count) {
        long hint = count == null || count <= 0 ? SCAN_BATCH_MAX_HINT
                : Math.min(count.longValue(), SCAN_BATCH_MAX_HINT);
        return ScanOptions.scanOptions().count(hint).build();
    }

    /**
     * 发布前拒绝空白通道，业务仍需自行保证通道命名空间隔离。
     *
     * <p>本方法只做非空校验；通道之间的命名空间隔离（如前缀约定）属于业务约定，
     * 框架层不强制拦截，避免限制合理的自定义通道。</p>
     *
     * @param channel Pub/Sub 通道名
     * @throws IllegalArgumentException 当 {@code channel} 为空白时抛出
     */
    private static void validateChannel(String channel) {
        if (StringUtils.isBlank(channel)) {
            throw new IllegalArgumentException("Redis Pub/Sub channel must not be blank");
        }
    }
}
