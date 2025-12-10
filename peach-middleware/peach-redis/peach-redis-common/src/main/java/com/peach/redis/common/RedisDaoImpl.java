

package com.peach.redis.common;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4 17:39
 */
@Slf4j
@Repository("redisDao")
public class RedisDaoImpl extends AbstractBaseRedisDao<Object, Object> implements RedisDao {

    //最大重试次数
    private static final Integer tryTimes = 3;

    @Value("${peach.redis.mode}")
    private String redisMode;
    private static final int USE_SCAN_COMMAND = 1;

    private static List<String> getScanResult(Jedis redisService, String key, Integer count) {
        Date startTime = new Date();
        //扫描的参数对象创建与封装
        ScanParams params = new ScanParams();
        params.match(key);
        //扫描返回一百行，这里可以根据业务需求进行修改
        params.count(count);
        String cursor = "0";
        ScanResult<String> scanResult = redisService.scan(cursor, params);
        //scan.getStringCursor() 存在 且不是 0 的时候，一直移动游标获取
//        List<String> list = new ArrayList<>();
//        while (null != scanResult.getCursor()) {
//            //封装扫描的结果
//            list.addAll(scanResult.getResult());
//            cursor = scanResult.getCursor();
//            if (!"0".equals(cursor)) {
//                scanResult = redisService.scan(cursor, params);
//            } else {
//                break;
//            }
//        }
        List<String> list = new ArrayList<>();
        //获取当前游标值
        cursor = scanResult.getCursor();
        //取得本轮scan的结果
        List<String> results = scanResult.getResult();
        if (results != null && results.size() > 0) {
            list.addAll(results);
        }
        // 如果游标值为0，表示已经遍历完所有键值对
        while (!"0".equals(cursor)) {
            // 更新游标并继续遍历下⼀轮
            scanResult = redisService.scan(cursor, params);
            cursor = scanResult.getCursor();
            results = scanResult.getResult();
            if (results != null && results.size() > 0) {
                list.addAll(results);
            }
            long timeOut = DateUtil.between(startTime, new Date(), DateUnit.SECOND);
            if (StringUtils.isBlank(cursor)) {
                log.info("cursor == null");
                cursor = "0";
            }
            if (timeOut >= 10) {
                log.info("redis scan timeOut 10s");
                cursor = "0";
            }
        }
        return list;
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
        while (retryNum <= tryTimes) {
            try {
                flag = Boolean.TRUE.equals(redisTemplate.delete(key));
                if (flag) {
                    break;
                }
                retryNum++;
            } catch (Exception e) {
                log.error("重试失败"+e.getMessage(),e);
                retryNum++;
            }
        }
        return flag;
    }

    @Override
    public long deletePattern(final Object pattern, final Integer count) {
        Set<Object> keys = scan(String.valueOf(pattern), count);
        if ((keys != null ? keys.size() : 0) > 0) {
//            return redisTemplate.delete(keys);
            return delete(keys);
        } else {
            return 0;
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
//        return redisTemplate.delete(keys);
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
        if (deleteKeys.size() > 0) {
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
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public boolean vSet(final Object key, Object value, Long expireTime) {
        boolean result = false;
        try {
            ValueOperations<Object, Object> operations = redisTemplate.opsForValue();
            operations.set(key, value);
            redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
    public List<?> lRange(Object k, long start, long end) {
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
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long size = list.remove(k, 0, null);
        return size == null ? 0 : size;
    }

    @Override
    public long lRemove(Object k, long count, Object v) {
        ListOperations<Object, Object> list = redisTemplate.opsForList();
        Long size = list.remove(k, count, v);
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
     * @param matchKey
     * @param count
     * @return Set
     * @Description: SCAN 命令用于迭代哈希键中的键值对。
     * @Date 2020-4-12 上午10:17:03
     */
    @Override
    public Set<Object> scan(String matchKey, Integer count) {
        return null;
//        NacosBaseConfig config = SpringUtil.getBean(NacosBaseConfig.class);
//        Integer scanLine;
//        if (count == null || count == 0) {
//            scanLine = config.getScanLine();
//        } else {
//            scanLine = count;
//        }
//        int useScan = config.getScanCommandIsUsed();
//        if ("standalone".equals(redisMode)) {
//            if (useScan == USE_SCAN_COMMAND) {
//                return redisTemplate.execute((RedisCallback<Set<Object>>) connection -> {
//                    Set<Object> keysTmp = new HashSet<>();
//                    Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions().match(matchKey).count(scanLine).build());
//                    while (cursor.hasNext()) {
//                        keysTmp.add(new String(cursor.next()));
//                    }
//                    return keysTmp;
//                });
//            } else {
//                return redisTemplate.keys(matchKey);
//            }
//        } else {
//            List<String> list = new ArrayList<>();
//            RedisClusterConnection redisClusterConnection = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getClusterConnection();
//            //这里是获取edispool的另外一种方式与上边的pipline可以对比下，两种方式都可以实现
//            Map<String, JedisPool> clusterNodes = ((JedisCluster) redisClusterConnection.getNativeConnection()).getClusterNodes();
//            for (Map.Entry<String, JedisPool> entry : clusterNodes.entrySet()) {
//                //获取单个的jedis对象
//                JedisPool jedisPool = entry.getValue();
//                Jedis jedis = jedisPool.getResource();
//                // 判断非从节点(因为若主从复制，从节点会跟随主节点的变化而变化)，此处要使用主节点从主节点获取数据
//                try {
//                    if (!jedis.info("replication").contains("role:slave")) {
//                        Collection<String> keys;
//                        if (useScan == USE_SCAN_COMMAND) {
//                            keys = getScanResult(jedis, matchKey, scanLine);
//                        } else {
//                            keys = jedis.keys(matchKey);
//                        }
//                        if (!keys.isEmpty()) {
//                            Map<Integer, List<String>> map = new HashMap<>(8);
//                            //接下来的循环不是多余的，需要注意
//                            for (String key : keys) {
//                                // cluster模式执行多key操作的时候，这些key必须在同一个slot上，不然会报:JedisDataException:
//                                int slot = JedisClusterCRC16.getSlot(key);
//                                // 按slot将key分组，相同slot的key一起提交
//                                if (map.containsKey(slot)) {
//                                    map.get(slot).add(key);
//                                } else {
//                                    List<String> list1 = new ArrayList<>();
//                                    list1.add(key);
//                                    map.put(slot, list1);
//                                }
//                            }
//                            for (Map.Entry<Integer, List<String>> integerListEntry : map.entrySet()) {
//                                list.addAll(integerListEntry.getValue());
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    log.error("redis error:" + e);
//                } finally {
//                    try {
//                        jedisPool.returnResource(jedis);
//                    } catch (Exception e) {
//                        log.error("jedisPool.returnResource error:" + e);
//                    }
//                }
//            }
//            return new HashSet<>(list);
//        }
    }

    @Override
    public Map hscan(String matchKey, Integer count) {

        Map<Object, Object> map = new HashMap<>();
        try {
            Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash().scan("field", ScanOptions.scanOptions().match(matchKey).count(count).build());
            while (cursor.hasNext()) {
                Object key = cursor.next().getKey();
                Object valueSet = cursor.next().getValue();
                map.put(key, valueSet);
            }
            //关闭cursor
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public Set<String> sscan(String matchKey, Integer count) {

        Set<String> keys = new HashSet<>();
        try {
            Cursor<Object> cursor = redisTemplate.opsForSet().scan("setValue", ScanOptions.scanOptions().match(matchKey).count(count).build());
            while (cursor.hasNext()) {
                keys.add(cursor.next().toString());
            }
            //关闭cursor
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return keys;
    }

    @Override
    public Map zscan(String matchKey, Integer count) {

        Map<Object, Object> map = new HashMap<>();
        try {
            Cursor<ZSetOperations.TypedTuple<Object>> cursor = redisTemplate.opsForZSet().scan("zSetValue", ScanOptions.scanOptions().match(matchKey).count(count).build());
            while (cursor.hasNext()) {
                ZSetOperations.TypedTuple<Object> typedTuple = cursor.next();
                Object value = typedTuple.getValue();
                Object valueScore = typedTuple.getScore();
                map.put(value, valueScore);
            }
            //关闭cursor
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }


    @Override
    public Set<Object> keys(String matchKey) {
//        return redisTemplate.keys(matchKey);
//        NacosBaseConfig config = SpringUtil.getBean(NacosBaseConfig.class);
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    @Override
    public void expire(Object k, long timeout) {
        try {
            redisTemplate.expire(k, timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public long increaseNum(String key, long fromIndex) {
        if (fromIndex < 0) {
            throw new RuntimeException("fromIndex不能小于0");
        }
        if (StringUtils.isBlank(key)) {
            throw new RuntimeException("key不能为空");
        }
        Long increment = redisTemplate.opsForValue().increment(key, fromIndex);
        return increment == null ? 0 : increment;
    }

    @Override
    public long decreaseNum(String key, long decIndex) {
        if (decIndex < 0) {
            throw new RuntimeException("decIndex不能小于0");
        }
        if (StringUtils.isBlank(key)) {
            throw new RuntimeException("key不能为空");
        }
        Long decrement = redisTemplate.opsForValue().decrement(key, decIndex);
        return decrement == null ? 0 : decrement;
    }
}
