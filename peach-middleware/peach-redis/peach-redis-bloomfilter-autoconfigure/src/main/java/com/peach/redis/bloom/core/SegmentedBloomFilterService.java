package com.peach.redis.bloom.core;


import com.peach.redis.bloom.config.BloomFilterProperties;
import com.peach.redis.bloom.spi.BloomScalePolicy;
import com.peach.redis.bloom.spi.CodecProvider;
import com.peach.redis.bloom.spi.KeyNamingStrategy;
import jodd.util.StringUtil;
import org.redisson.api.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 分段可扩容的 BloomFilter 实现：
 * - 写入统一进入“尾段”，达到负载阈值时新增下一段；
 * - 读取跨所有段，优先尾段（新→旧），提高命中率与性能；
 * - 并发扩容与初始化通过命名空间级分布式锁保护；
 * - 本地缓存（可选）减少远程调用；
 * - 容量元数据存储于 Redis Map，确保负载计算使用真实容量。
 */
public class SegmentedBloomFilterService implements BloomFilterService {

    private final RedissonClient redisson;
    private final BloomFilterProperties props;
    private final CodecProvider codecProvider;
    private final KeyNamingStrategy keyNaming;
    private final BloomScalePolicy scalePolicy;

    // Local caches to reduce Redis round-trips
    private final Map<String, List<String>> segCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, RBloomFilter<Object>> bfCache = new java.util.concurrent.ConcurrentHashMap<>();

    public SegmentedBloomFilterService(RedissonClient redisson,
                                       BloomFilterProperties props,
                                       CodecProvider codecProvider,
                                       KeyNamingStrategy keyNaming,
                                       BloomScalePolicy scalePolicy) {
        this.redisson = redisson;
        this.props = props;
        this.codecProvider = codecProvider;
        this.keyNaming = keyNaming;
        this.scalePolicy = scalePolicy;
    }

    @Override
    public boolean add(String namespace, Object value) {
        String ns = resolveNamespace(namespace);
        RBloomFilter<Object> tail = getOrCreateTail(ns);
        boolean added = tail.add(value);
        if (added) afterAdd(ns);
        return added;
    }

    @Override
    public boolean mightContain(String namespace, Object value) {
        String ns = resolveNamespace(namespace);
        List<RBloomFilter<Object>> segments = allSegments(ns);
        for (RBloomFilter<Object> bf : segments) {
            if (bf.contains(value)) return true;
        }
        return false;
    }

    @Override
    public void addAll(String namespace, Collection<?> values) {
        if (values == null || values.isEmpty()) return;
        String ns = resolveNamespace(namespace);
        RBloomFilter<Object> tail = getOrCreateTail(ns);
        values.stream().forEach(x->tail.add(x));
        // approximate count increment
        incrementCount(ns, values.size());
        ensureScaleIfNeeded(ns);
    }

    @Override
    public boolean mightContainAll(String namespace, Collection<?> values) {
        if (values == null || values.isEmpty()) return true;
        String ns = resolveNamespace(namespace);
        List<RBloomFilter<Object>> segments = allSegments(ns);
        for (Object v : values) {
            boolean found = false;
            for (RBloomFilter<Object> bf : segments) {
                if (bf.contains(v)) { found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }

    @Override
    public void clear(String namespace) {
        String ns = resolveNamespace(namespace);
        String segmentsKey = keyNaming.segmentsKey(props.getKeyPrefix(), ns);
        RLock lock = redisson.getLock(keyNaming.lockKey(props.getKeyPrefix(), ns));
        lock.lock();
        try {
            RList<String> segs = redisson.getList(segmentsKey);
            for (String segName : segs) {
                RBloomFilter<Object> bf = getBloomFilterCached(segName);
                bf.delete();
                getCount(ns, segName).delete();
            }
            segs.delete();
            // delete metadata maps
            redisson.getMap(keyNaming.capacityMapKey(props.getKeyPrefix(), ns)).delete();
            redisson.getMap(keyNaming.fppMapKey(props.getKeyPrefix(), ns)).delete();
            // invalidate local caches
            segCache.remove(ns);
            // remove filter handles for this namespace
            List<String> namesToRemove = new ArrayList<>(bfCache.keySet());
            for (String name : namesToRemove) {
                if (name.startsWith(props.getKeyPrefix() + ":" + ns + ":")) {
                    bfCache.remove(name);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int segments(String namespace) {
        String ns = resolveNamespace(namespace);
        RList<String> segs = redisson.getList(keyNaming.segmentsKey(props.getKeyPrefix(), ns));
        return segs.size();
    }

    private String resolveNamespace(String ns) {
        return (ns == null || StringUtil.isBlank(ns)) ? props.getDefaultNamespace() : ns;
    }

    /**
     * 获取或创建尾段（必要时进行初始段创建）。
     * 使用命名空间锁避免并发下多次创建同一首段。
     */
    private RBloomFilter<Object> getOrCreateTail(String ns) {
        String segmentsKey = keyNaming.segmentsKey(props.getKeyPrefix(), ns);
        RList<String> segs = redisson.getList(segmentsKey);
        if (segs.isEmpty()) {
            RLock lock = redisson.getLock(keyNaming.lockKey(props.getKeyPrefix(), ns));
            lock.lock();
            try {
                if (segs.isEmpty()) {
                    String segName = keyNaming.segmentName(props.getKeyPrefix(), ns, 0);
                    RBloomFilter<Object> bf = getBloomFilterCached(segName);
                    bf.tryInit(props.getInitialCapacity(), props.getFalsePositiveProbability());
                    segs.add(segName);
                    getCount(ns, segName).set(0);
                    // record capacity & fpp metadata
                    redisson.getMap(keyNaming.capacityMapKey(props.getKeyPrefix(), ns)).put(segName, props.getInitialCapacity());
                    redisson.getMap(keyNaming.fppMapKey(props.getKeyPrefix(), ns)).put(segName, props.getFalsePositiveProbability());
                    if (props.isEnableLocalCache()) {
                        segCache.put(ns, new ArrayList<>(segs.readAll()));
                    }
                    return bf;
                }
            } finally {
                lock.unlock();
            }
        }
        String tailName = segs.get(segs.size() - 1);
        return getBloomFilterCached(tailName);
    }

    /**
     * 获取当前命名空间的所有段对应的 BloomFilter 句柄，返回顺序为新→旧。
     * 在启用本地缓存时，优先使用缓存的段名快照。
     */
    private List<RBloomFilter<Object>> allSegments(String ns) {
        List<String> names;
        if (props.isEnableLocalCache()) {
            names = segCache.get(ns);
            if (names == null) {
                RList<String> rlist = redisson.getList(keyNaming.segmentsKey(props.getKeyPrefix(), ns));
                names = new ArrayList<>(rlist.readAll());
                segCache.put(ns, names);
            }
        } else {
            RList<String> rlist = redisson.getList(keyNaming.segmentsKey(props.getKeyPrefix(), ns));
            names = new ArrayList<>(rlist.readAll());
        }
        // Prefer tail-first (new -> old) for better hit rate
        Collections.reverse(names);
        return names.stream().map(this::getBloomFilterCached).collect(Collectors.toList());
    }

    private void afterAdd(String ns) {
        String segmentsKey = keyNaming.segmentsKey(props.getKeyPrefix(), ns);
        RList<String> segs = redisson.getList(segmentsKey);
        if (segs.isEmpty()) return;
        String tailName = segs.get(segs.size() - 1);
        incrementCount(ns, 1);
        ensureScaleIfNeeded(ns);
    }

    private void incrementCount(String ns, long delta) {
        String segmentsKey = keyNaming.segmentsKey(props.getKeyPrefix(), ns);
        RList<String> segs = redisson.getList(segmentsKey);
        if (segs.isEmpty()) return;
        String tailName = segs.get(segs.size() - 1);
        RAtomicLong cnt = getCount(ns, tailName);
        cnt.addAndGet(delta);
    }

    private RAtomicLong getCount(String ns, String segmentName) {
        return redisson.getAtomicLong(keyNaming.segmentCountKey(props.getKeyPrefix(), ns, segmentName));
    }

    /**
     * 当尾段负载达到阈值时触发扩容：
     * - 在锁保护下二次检查，避免并发重复扩容；
     * - 新增下一段，并初始化计数与容量元数据；
     * - 刷新本地缓存段名快照。
     */
    private void ensureScaleIfNeeded(String ns) {
        String segmentsKey = keyNaming.segmentsKey(props.getKeyPrefix(), ns);
        RList<String> segs = redisson.getList(segmentsKey);
        if (segs.isEmpty()) return;
        String tailName = segs.get(segs.size() - 1);
        RBloomFilter<Object> tail = getBloomFilterCached(tailName);
        // read real capacity from capacity map
        RMap<String, Long> capMap = redisson.getMap(keyNaming.capacityMapKey(props.getKeyPrefix(), ns));
        Long capacity = capMap.get(tailName);
        if (capacity == null || capacity <= 0) {
            capacity = (long) Math.ceil(props.getInitialCapacity() * Math.pow(props.getScaleFactor(), Math.max(0, segs.size() - 1)));
            capMap.put(tailName, capacity);
        }
        long count = getCount(ns, tailName).get();

        double load = (capacity == 0) ? 1.0 : (double) count / (double) capacity;
        if (load >= props.getLoadFactor() && segs.size() < props.getMaxSegments()) {
            RLock lock = redisson.getLock(keyNaming.lockKey(props.getKeyPrefix(), ns));
            lock.lock();
            try {
                // re-read to avoid race
                segs = redisson.getList(segmentsKey);
                String currentTail = segs.get(segs.size() - 1);
                if (!Objects.equals(currentTail, tailName)) return; // another scaling happened

                long nextCapacity = scalePolicy.nextCapacity(capacity, props.getScaleFactor());
                String nextName = keyNaming.segmentName(props.getKeyPrefix(), ns, segs.size());
                RBloomFilter<Object> next = getBloomFilterCached(nextName);
                next.tryInit(nextCapacity, props.getFalsePositiveProbability());
                segs.add(nextName);
                getCount(ns, nextName).set(0);
                // write capacity & fpp metadata
                redisson.getMap(keyNaming.capacityMapKey(props.getKeyPrefix(), ns)).put(nextName, nextCapacity);
                redisson.getMap(keyNaming.fppMapKey(props.getKeyPrefix(), ns)).put(nextName, props.getFalsePositiveProbability());
                if (props.isEnableLocalCache()) {
                    segCache.put(ns, new ArrayList<>(segs.readAll()));
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 获取 BloomFilter 句柄；启用本地缓存时将句柄缓存于进程内，降低重复构造成本。
     */
    private RBloomFilter<Object> getBloomFilterCached(String name) {
        if (!props.isEnableLocalCache()) {
            return redisson.getBloomFilter(name, codecProvider.codec());
        }
        return bfCache.computeIfAbsent(name, n -> redisson.getBloomFilter(n, codecProvider.codec()));
    }

    @Override
    public void initNamespace(String namespace, Long initialCapacityOverride, Double falsePositiveProbabilityOverride) {
        String ns = resolveNamespace(namespace);
        String segmentsKey = keyNaming.segmentsKey(props.getKeyPrefix(), ns);
        RList<String> segs = redisson.getList(segmentsKey);
        if (segs.isEmpty()) {
            RLock lock = redisson.getLock(keyNaming.lockKey(props.getKeyPrefix(), ns));
            lock.lock();
            try {
                if (segs.isEmpty()) {
                    String segName = keyNaming.segmentName(props.getKeyPrefix(), ns, 0);
                    RBloomFilter<Object> bf = getBloomFilterCached(segName);
                    long cap = initialCapacityOverride != null && initialCapacityOverride > 0 ? initialCapacityOverride : props.getInitialCapacity();
                    double fpp = falsePositiveProbabilityOverride != null && falsePositiveProbabilityOverride > 0 ? falsePositiveProbabilityOverride : props.getFalsePositiveProbability();
                    bf.tryInit(cap, fpp);
                    segs.add(segName);
                    getCount(ns, segName).set(0);
                    // record capacity metadata
                    redisson.getMap(keyNaming.capacityMapKey(props.getKeyPrefix(), ns)).put(segName, cap);
                    // record fpp metadata
                    redisson.getMap(keyNaming.fppMapKey(props.getKeyPrefix(), ns)).put(segName, fpp);
                    if (props.isEnableLocalCache()) {
                        segCache.put(ns, new ArrayList<>(segs.readAll()));
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public BloomStatus status(String namespace) {
        String ns = resolveNamespace(namespace);
        String segmentsKey = keyNaming.segmentsKey(props.getKeyPrefix(), ns);
        RList<String> segs = redisson.getList(segmentsKey);
        List<String> names = new ArrayList<>(segs.readAll());
        BloomStatus out = new BloomStatus();
        out.namespace = ns;
        out.segments = names.size();
        out.tailSegment = names.isEmpty() ? null : names.get(names.size() - 1);
        List<BloomStatus.SegmentStatus> sList = new ArrayList<>();
        RMap<String, Long> capMap = redisson.getMap(keyNaming.capacityMapKey(props.getKeyPrefix(), ns));
        RMap<String, Double> fppMap = redisson.getMap(keyNaming.fppMapKey(props.getKeyPrefix(), ns));

        long totalCap = 0L;
        long totalCnt = 0L;
        double productOneMinusFpp = 1.0d;

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            long cap = Optional.ofNullable(capMap.get(name)).orElse(0L);
            long cnt = getCount(ns, name).get();
            double fpp = Optional.ofNullable(fppMap.get(name)).orElse(props.getFalsePositiveProbability());
            BloomStatus.SegmentStatus ss = new BloomStatus.SegmentStatus();
            ss.name = name;
            ss.index = i;
            ss.capacity = cap;
            ss.countApprox = cnt;
            ss.load = cap == 0 ? 0.0d : (double) cnt / (double) cap;
            ss.fpp = fpp;
            sList.add(ss);
            totalCap += cap;
            totalCnt += cnt;
            productOneMinusFpp *= (1.0d - Math.max(0.0d, Math.min(1.0d, fpp)));
        }

        out.segmentStatuses = sList;
        out.totalCapacity = totalCap;
        out.totalCountApprox = totalCnt;
        out.effectiveFpp = 1.0d - productOneMinusFpp;
        return out;
    }
}