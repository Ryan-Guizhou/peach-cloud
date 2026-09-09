package com.peach.common.unique.impl;

import com.peach.common.unique.UniqueGenerator;
import com.peach.common.unique.UniqueGeneratorConst;
import com.peach.common.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * 64 位雪花算法（Snowflake）分布式唯一 ID 生成器。
 *
 * <p>ID 二进制结构划分（共 64 位）：
 * <ul>
 *   <li>1 位符号位：恒为 0，保证生成的 ID 为正长整数。</li>
 *   <li>41 位时间戳：毫秒级时间差（当前时间 - 起始基准时间 {@link #START_TIMESTAMP}），支持约 69 年。</li>
 *   <li>5 位数据中心 ID：取值范围 0~31（最多支持 32 个数据中心）。</li>
 *   <li>5 位工作机器 ID：取值范围 0~31（最多支持每个数据中心 32 台机器节点）。</li>
 *   <li>12 位递增序列号：毫秒内自增，取值范围 0~4095（单节点单毫秒最大支持 4096 个并发 ID）。</li>
 * </ul>
 * </p>
 *
 * <p>时钟保护与兜底策略：
 * <ul>
 *   <li>毫秒内并发超限：自旋等待至下一毫秒生成。</li>
 *   <li>小幅时钟回拨（&le; 5ms）：线程主动休眠等待时钟追平后继续生成。</li>
 *   <li>大幅时钟回拨（&gt; 5ms）：拒绝生成并抛出 {@link IllegalStateException}，防止产生重复 ID。</li>
 *   <li>workerId / datacenterId 多级自动解析：系统属性 &rarr; 环境变量 &rarr; 本机 IP/MAC 哈希推导 &rarr; 最终默认兜底值。</li>
 * </ul>
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/8 16:15
 */
public class SnowflakeUniqueGenerator implements UniqueGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeUniqueGenerator.class);

    /**
     * 起始时间戳基准（2024-01-01 00:00:00 UTC，单位：毫秒）。
     */
    private static final long START_TIMESTAMP = 1704067200000L;

    /**
     * 序列号所占位数：12 位。
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 工作机器 ID 所占位数：5 位。
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 数据中心 ID 所占位数：5 位。
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 支持的最大机器 ID：31。
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 支持的最大数据中心 ID：31。
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 序列号掩码：4095 (0b111111111111)。
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器 ID 向左移位数：12 位。
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心 ID 向左移位数：17 位 (12 + 5)。
     */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳向左移位数：22 位 (12 + 5 + 5)。
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 允许容忍的小幅时钟回拨阈值（单位：毫秒）。
     */
    private static final long MAX_BACKWARD_MS = 5L;

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 无参构造器，按系统属性 &rarr; 环境变量 &rarr; 本机网络特征 &rarr; 默认值顺序自动推导 workerId 与 datacenterId。
     */
    public SnowflakeUniqueGenerator() {
        this(resolveWorkerId(), resolveDatacenterId());
    }

    /**
     * 显式指定 workerId 和 datacenterId 的构造器。
     *
     * @param workerId     工作机器 ID（0 ~ 31）
     * @param datacenterId 数据中心 ID（0 ~ 31）
     * @throws IllegalArgumentException 当 workerId 或 datacenterId 超出取值范围 [0, 31] 时抛出
     */
    public SnowflakeUniqueGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        log.debug("Initialized SnowflakeIdGenerator with workerId={}, datacenterId={}", workerId, datacenterId);
    }

    /**
     * 生成下一个字符串形式的雪花 ID。
     *
     * @return 64 位整型的十进制数字字符串（通常为 19 位）
     */
    @Override
    public String nextId() {
        return Long.toString(nextLongId());
    }

    /**
     * 生成下一个长整型数值雪花 ID。
     *
     * @return 64 位长整型唯一 ID
     * @throws IllegalStateException 当发生不可恢复的时钟回拨或线程等待被中断时抛出
     */
    @Override
    public synchronized long nextLongId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= MAX_BACKWARD_MS) {
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new IllegalStateException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for clock to catch up", e);
                }
            } else {
                throw new IllegalStateException(String.format("Clock moved backwards too far (%d ms). Refusing to generate id", offset));
            }
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    @Override
    public String type() {
        return UniqueGeneratorConst.SNOWFLAKE;
    }

    /**
     * 当前配置的机器 ID。
     *
     * @return workerId
     */
    public long getWorkerId() {
        return workerId;
    }

    /**
     * 当前配置的数据中心 ID。
     *
     * @return datacenterId
     */
    public long getDatacenterId() {
        return datacenterId;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 多级解析 workerId：系统属性 &rarr; 环境变量 &rarr; 网卡/IP 推导 &rarr; 默认值。
     */
    private static long resolveWorkerId() {
        // 1. 系统属性
        String prop = System.getProperty(UniqueGeneratorConst.PROPERTY_SNOWFLAKE_WORKER_ID);
        if (StringUtil.isNotBlank(prop)) {
            return parseConfiguredId(prop, UniqueGeneratorConst.PROPERTY_SNOWFLAKE_WORKER_ID, MAX_WORKER_ID);
        }
        // 2. 环境变量（标准命名）
        String env = System.getenv(UniqueGeneratorConst.ENV_SNOWFLAKE_WORKER_ID);
        if (StringUtil.isNotBlank(env)) {
            return parseConfiguredId(env, UniqueGeneratorConst.ENV_SNOWFLAKE_WORKER_ID, MAX_WORKER_ID);
        }
        // 3. 环境变量（简写兼容命名）
        String envShort = System.getenv(UniqueGeneratorConst.ENV_SNOWFLAKE_WORKER_ID_SHORT);
        if (StringUtil.isNotBlank(envShort)) {
            return parseConfiguredId(envShort, UniqueGeneratorConst.ENV_SNOWFLAKE_WORKER_ID_SHORT, MAX_WORKER_ID);
        }
        // 4. 网卡与 IP 特征自适应计算
        return autoDetectWorkerId();
    }

    /**
     * 多级解析 datacenterId：系统属性 &rarr; 环境变量 &rarr; IP 特征推导 &rarr; 默认值。
     */
    private static long resolveDatacenterId() {
        // 1. 系统属性
        String prop = System.getProperty(UniqueGeneratorConst.PROPERTY_SNOWFLAKE_DATACENTER_ID);
        if (StringUtil.isNotBlank(prop)) {
            return parseConfiguredId(prop, UniqueGeneratorConst.PROPERTY_SNOWFLAKE_DATACENTER_ID, MAX_DATACENTER_ID);
        }
        // 2. 环境变量（标准命名）
        String env = System.getenv(UniqueGeneratorConst.ENV_SNOWFLAKE_DATACENTER_ID);
        if (StringUtil.isNotBlank(env)) {
            return parseConfiguredId(env, UniqueGeneratorConst.ENV_SNOWFLAKE_DATACENTER_ID, MAX_DATACENTER_ID);
        }
        // 3. 环境变量（简写兼容命名）
        String envShort = System.getenv(UniqueGeneratorConst.ENV_SNOWFLAKE_DATACENTER_ID_SHORT);
        if (StringUtil.isNotBlank(envShort)) {
            return parseConfiguredId(envShort, UniqueGeneratorConst.ENV_SNOWFLAKE_DATACENTER_ID_SHORT, MAX_DATACENTER_ID);
        }
        // 4. IP 特征自适应计算
        return autoDetectDatacenterId();
    }

    private static long autoDetectWorkerId() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network != null && network.getHardwareAddress() != null) {
                byte[] mac = network.getHardwareAddress();
                return ((0x000000FF & (long) mac[mac.length - 2])
                        | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6 & MAX_WORKER_ID;
            }
            byte[] address = ip.getAddress();
            return (address[address.length - 1] & 0x1F) & MAX_WORKER_ID;
        } catch (Exception ex) {
            return UniqueGeneratorConst.DEFAULT_WORKER_ID;
        }
    }

    private static long parseConfiguredId(String rawValue, String configKey, long maxValue) {
        try {
            long value = Long.parseLong(rawValue.trim());
            if (value < 0 || value > maxValue) {
                throw new IllegalArgumentException(configKey + " must be between 0 and " + maxValue);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(configKey + " must be a valid long number", exception);
        }
    }

    private static long autoDetectDatacenterId() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            byte[] address = ip.getAddress();
            return (address[address.length - 2] & 0x1F) & MAX_DATACENTER_ID;
        } catch (Exception ex) {
            return UniqueGeneratorConst.DEFAULT_DATACENTER_ID;
        }
    }
}
