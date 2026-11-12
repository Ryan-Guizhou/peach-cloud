package com.peach.redis.tool.quickstart.example;

import com.peach.redis.tool.RedisDao;
import com.peach.redis.tool.quickstart.config.RedisToolDemoContext;
import com.peach.redis.tool.quickstart.enums.RedisToolKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * RedisDao 全操作演示；每组使用独立数据，校验实际执行结果，失败即停止。
 * 删除、弹出及裁剪属于演示本身，不受后置清理开关影响。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 16:00
 */
@Component
@RequiredArgsConstructor
public class RedisToolOperationsExample {
    private static final Duration TTL = Duration.ofMinutes(30);
    private final RedisDao redisDao;
    private final RedisToolDemoContext context;
    private final RedisConnectionFactory connectionFactory;

    /** 先登记全部数据键，使前置清理发生在任何业务写入之前。 */
    public void prepare() {
        for (RedisToolKey definition : List.of(RedisToolKey.VALUE, RedisToolKey.HASH,
                RedisToolKey.LIST, RedisToolKey.SET, RedisToolKey.ZSET, RedisToolKey.DELETE)) {
            for (String id : List.of("main", "other", "missing")) {
                context.key(definition, id);
            }
        }
    }

    public void run() throws Exception {
        stringAndTtl();
        hash();
        list();
        set();
        sortedSet();
        keyAndScan();
        publishAndSubscribe();
    }

    private String key(RedisToolKey definition, String id) {
        return context.key(definition, id);
    }

    private void stringAndTtl() {
        String key = key(RedisToolKey.VALUE, "main");
        String other = key(RedisToolKey.VALUE, "other");
        String missing = key(RedisToolKey.VALUE, "missing");
        check(redisDao.vSet(key, "initial"), "vSet");
        check(redisDao.ttl(key) == -1, "persistent TTL");
        check(redisDao.expire(key, Duration.ofSeconds(60)), "expire existing key");
        check(redisDao.vSet(key, "seconds", 60L), "vSet seconds");
        check(redisDao.vSet(key, "duration", TTL), "vSet Duration");
        check("duration".equals(redisDao.vGet(key)), "vGet");
        check(redisDao.vMultiGet(List.of(key, missing)).equals(Arrays.asList("duration", null)), "vMultiGet");
        check(!redisDao.vSetIfAbsent(key, "ignored", TTL), "SET NX existing");
        check(redisDao.vSetIfAbsent(other, "created", TTL), "SET NX missing");
        check(redisDao.vSetIfPresent(other, "updated", TTL), "SET XX existing");
        check(!redisDao.vSetIfPresent(missing, "ignored", TTL), "SET XX missing");
        check("updated".equals(redisDao.vGetAndDelete(other)), "GETDEL");
        check(redisDao.ttl(other) == -2, "missing TTL");
        check(redisDao.expire(key, TTL), "expire Duration");
        check(redisDao.ttl(key) > 0, "positive TTL");
        check(redisDao.persist(key), "persist");
        redisDao.expire(key, Duration.ofSeconds(60));
        check(redisDao.existsKey(key), "existsKey");
        check(redisDao.increaseNum(other, 2) == 2, "increaseNum");
        check(redisDao.decreaseNum(other, 1) == 1, "decreaseNum");
        check(((Number) redisDao.vGet(other)).longValue() == 1L, "counter serializer");
    }

    private void hash() {
        String key = key(RedisToolKey.HASH, "main");
        redisDao.hmSet(key, "name", "Peach");
        redisDao.hmSetAll(key, Map.of("city", "Shanghai", "level", 1));
        check("Peach".equals(redisDao.hmGet(key, "name")), "hmGet field");
        check(redisDao.hmGet(key).size() == 3, "hmGet all");
        check(redisDao.hmHasKey(key, "city") && redisDao.hmSize(key) == 3, "hash metadata");
        check(redisDao.hmMultiGet(key, List.of("name", "missing")).equals(Arrays.asList("Peach", null)), "hmMultiGet");
        redisDao.hmIncrement(key, "visits", 1L);
        check(redisDao.hmIncrement(key, "visits", 2L) == 3, "hash integer increment");
        check(redisDao.hmIncrement(key, "rating", 0.5) == 0.5, "hash double increment");
        check(redisDao.hmScan(key, 10).size() == 5, "hash scans");
        check(redisDao.hmDelete(key, "city") == 1, "hmDelete single field");
        check(redisDao.hmDelete(key, "level", "missing") == 1, "hmDelete");
    }

    private void list() {
        String key = key(RedisToolKey.LIST, "main");
        redisDao.lLeftPush(key, "a");
        redisDao.lRightPush(key, "b");
        redisDao.lLeftPushAll(key, List.of("c", "d"));
        redisDao.lRightPushAll(key, List.of("e", "f"));
        check(redisDao.lSize(key) == 6, "lSize");
        check("d".equals(redisDao.lindex(key, 0)), "left index");
        check("f".equals(redisDao.lindex(key, -1)), "right index");
        check("c".equals(redisDao.lindex(key, 1)), "lindex");
        check(redisDao.lRange(key, 0, -1).size() == 6, "lRange all");
        check(redisDao.lRange(key, 0, 1).equals(List.of("d", "c")), "lRange bounds");
        redisDao.lRemove(key, 0, "a");
        redisDao.lLeftPush(key, "a");
        redisDao.lRemove(key, 0, "b");
        redisDao.lRightPush(key, "b");
        check("a".equals(redisDao.lLeftPop(key)), "lLeftPop");
        check("b".equals(redisDao.lRightPop(key)), "lRightPop");
        redisDao.lSet(key, 0, "changed");
        check(redisDao.lRemove(key, 1L, "changed") == 1, "lRemove count/value");
        check(redisDao.lRemove(key, 0, "c") == 1, "lRemove all matching");
        redisDao.lTrim(key, 0, 0);
        check(redisDao.lSize(key) == 1, "lTrim");

    }

    private void set() {
        String key = key(RedisToolKey.SET, "main");
        String other = key(RedisToolKey.SET, "other");
        redisDao.sAdd(key, "a");
        check(redisDao.sAddAll(key, "a", "b", "c") == 2, "sAddAll");
        redisDao.sAddAll(other, "b", "d");
        check(redisDao.sIsMember(key, "a") && redisDao.sSize(key) == 3, "set metadata");
        check(redisDao.sMembers(key).size() == 3, "sMembers");
        check(redisDao.sScan(key, 10).size() == 3, "set scans");
        check(redisDao.sIntersect(key, other).equals(Set.of("b")), "sIntersect");
        check(redisDao.sUnion(key, other).equals(Set.of("a", "b", "c", "d")), "sUnion");
        check(redisDao.sDifference(key, other).equals(Set.of("a", "c")), "sDifference");
        check(redisDao.sRandomMember(key) != null, "sRandomMember");
        check(redisDao.sPop(key) != null, "sPop");
        redisDao.sRemove(key, "a", "b", "c");
        redisDao.sAddAll(key, "retained");
    }

    private void sortedSet() {
        String key = key(RedisToolKey.ZSET, "main");
        redisDao.zAdd(key, "a", 1);
        check(redisDao.zAddAll(key, Map.of("b", 2.0, "c", 3.0)) == 2, "zAddAll");
        check(redisDao.zScore(key, "b") == 2.0, "zScore");
        check(redisDao.zIncrementScore(key, "a", 0.5) == 1.5, "zIncrementScore");
        check(redisDao.zRank(key, "a") == 0 && redisDao.zReverseRank(key, "a") == 2, "zRank");
        check(List.copyOf(redisDao.zRange(key, 0, -1)).equals(List.of("a", "b", "c")), "zRange");
        check(List.copyOf(redisDao.zReverseRange(key, 0, -1)).equals(List.of("c", "b", "a")), "zReverseRange");
        check(redisDao.zRangeWithScores(key, 0, -1).size() == 3, "zRangeWithScores");
        check(redisDao.zRangeByScore(key, 1, 2, 0, 10).size() == 2, "zRangeByScore bounded");
        check(redisDao.zRangeByScore(key, 0, 4, 1, 1).equals(Set.of("b")), "zRangeByScore page");
        check(redisDao.zReverseRangeByScore(key, 0, 4, 0, 1).equals(Set.of("c")), "zReverseRangeByScore page");
        check(redisDao.zSize(key) == 3 && redisDao.zCount(key, 1, 2) == 2, "zset count");
        check(redisDao.zScan(key, 10).size() == 3, "zset scans");
        check(redisDao.zRemoveRange(key, 0, 0) == 1, "zRemoveRange");
        check(redisDao.zRemoveRangeByScore(key, 3, 3) == 1, "zRemoveRangeByScore");
        check(redisDao.zRemove(key, "b", "missing") == 1, "zRemove");
        redisDao.zAdd(key, "retained", 1);
    }

    private void keyAndScan() {
        String first = key(RedisToolKey.DELETE, "main");
        String second = key(RedisToolKey.DELETE, "other");
        String third = key(RedisToolKey.DELETE, "missing");
        redisDao.vSet(first, "one");
        redisDao.vSet(second, "two");
        check(redisDao.scan(context.deletePattern(), 10).equals(Set.of(first, second)), "scan");
        check(redisDao.deletePattern(context.deletePattern(), 10) == 2, "deletePattern");
        redisDao.vSet(first, "one");
        redisDao.delete(Set.of(first));
        redisDao.vSet(second, "two");
        check(redisDao.delete(Set.of(second)) == 1, "delete Set");
        redisDao.vSet(third, "three");
        check(redisDao.delete((Object) third), "delete Object");
    }

    private void publishAndSubscribe() throws Exception {
        CountDownLatch received = new CountDownLatch(2);
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> received.countDown(), new ChannelTopic(context.channel()));
        try {
            container.afterPropertiesSet();
            container.start();
            redisDao.convertAndSend(context.channel(), "demo-event");
            redisDao.convertAndSend(context.channel(), Map.of("event", "demo"));
            check(received.await(5, TimeUnit.SECONDS), "Pub/Sub delivery");
        } finally {
            container.destroy();
        }
    }

    private static void check(boolean result, String operation) {
        if (!result) {
            throw new IllegalStateException("Redis tool demo verification failed: " + operation);
        }
    }
}
