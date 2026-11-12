package com.peach.redission.common;

import com.peach.common.util.StringUtil;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * RedissionData处理器。
 *
 * <p>对 {@link RedissonClient} 的轻量封装，提供基于 Redis Bucket（字符串/对象存储）
 * 的常用 KV 操作：设置、获取、删除、存在性判断、过期时间设置等。</p>
 *
 * <p>说明：</p>
 * <ul>
 *     <li>所有操作均直接委托给 Redisson 的 {@code RBucket}，本身不做本地缓存。</li>
 *     <li>{@link #get(String)} 统一把结果转成 String，非字符串类型会丢失原始类型信息。</li>
 *     <li>本类无状态，线程安全，可作为单例 Bean 注入使用。</li>
 * </ul>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/19 18:53
 */
public class RedissionDataHandle {

    /**
     * Redisson 客户端，所有 Bucket 操作的实际执行者。
     * 由构造器注入，外部保证其已初始化且可用。
     */
    private final RedissonClient redissonClient;

    /**
     * 构造 RedissionDataHandle。
     *
     * @param redissonClient Redisson 客户端，不能为 null
     */
    public RedissionDataHandle(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 设置 key 对应的值，不设置过期时间（永久有效，除非被显式删除）。
     *
     * @param key   Redis key
     * @param value 要存储的值，可为任意可序列化对象
     */
    public void set(String key, Object value) {
        redissonClient.getBucket(key).set(value);
    }

    /**
     * 获取 key 对应的值，并统一转换为 String 返回。
     *
     * <p>注意：底层使用 {@link StringUtil#getStringValue(Object)} 做字符串化，
     * 如果写入的是非字符串对象（如 POJO、集合），此处返回的是其字符串表示，
     * 无法反序列化回原对象。需要保留类型时，请直接使用 Redisson 的
     * {@code getBucket(key).get()} 并自行做类型转换。</p>
     *
     * @param key Redis key
     * @return key 对应的字符串值；key 不存在时返回 {@code StringUtil} 约定的空值表示
     */
    public String get(String key) {
        return StringUtil.getStringValue(redissonClient.getBucket(key).get());
    }

    /**
     * 删除指定 key。
     *
     * <p>key 不存在时不会抛异常，属于幂等操作。</p>
     *
     * @param key Redis key
     */
    public void delete(String key) {
        redissonClient.getBucket(key).delete();
    }

    /**
     * 判断指定 key 是否存在。
     *
     * @param key Redis key
     * @return 存在返回 {@code true}，否则返回 {@code false}
     */
    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 为已存在的 key 设置过期时间，单位固定为秒。
     *
     * <p>注意：Redisson 的 {@code expire} 只对已存在的 key 生效；
     * 如果 key 不存在，该调用不会创建 key，也不会抛异常，实际不产生效果。</p>
     *
     * @param key  Redis key
     * @param time 过期时间，单位：秒
     */
    public void expire(String key, long time) {
        redissonClient.getBucket(key).expire(getDuration(time, TimeUnit.SECONDS));
    }

    /**
     * 设置 key 对应的值，并同时指定过期时间。
     *
     * <p>与 {@link #set(String, Object)} 的区别是这里会带上 TTL，
     * 到期后 key 自动删除。</p>
     *
     * @param key      Redis key
     * @param value    要存储的值
     * @param time     过期时间数值，必须大于 0
     * @param timeUnit 过期时间单位，不能为 null
     */
    public void set(String key, Object value, long time, TimeUnit timeUnit) {
        redissonClient.getBucket(key).set(value, getDuration(time, timeUnit));
    }

    /**
     * 将「时间数值 + 时间单位」转换为 {@link Duration}。
     *
     * <p>实现说明：通过 {@link TimeUnit#toChronoUnit()} 把 {@link TimeUnit}
     * 映射为 {@link java.time.temporal.ChronoUnit}，再交给 {@link Duration#of(long, java.time.temporal.TemporalUnit)}。
     * 该转换要求 JDK 9+；且 {@link TimeUnit} 的 NANOSECONDS/MICROSECONDS 等
     * 均能映射到对应的 ChronoUnit，正常使用不会出问题。</p>
     *
     * <p>注意：入参 {@code timeToLive} 命名与调用方参数 {@code time} 不一致，
     * 语义上是「存活时长」，调用时需保证为正数，负数会导致 Duration 为负，
     * 进而使 Redis 侧 TTL 行为不符合预期。</p>
     *
     * @param timeToLive 时间数值
     * @param timeUnit   时间单位，为 null 时抛出 {@link NullPointerException}
     * @return 对应的 {@link Duration}
     */
    public Duration getDuration(long timeToLive, TimeUnit timeUnit) {
        return Duration.of(timeToLive, Objects.requireNonNull(timeUnit).toChronoUnit());
    }
}
