package com.peach.redis.tool;

import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 同步 Redis 数据访问契约，覆盖键管理与常用数据结构操作。
 *
 * <p>Key 由业务定义并通过 KeyBuilder 构建后传入；Object 类型键参数仅保留历史签名，
 * 实际使用字符串键序列化器。值和 Hash 值沿用应用统一序列化器，不支持 null 值。
 * 接口不保存业务状态，实现复用线程安全的 RedisTemplate。</p>
 *
 * <p>所有方法均在调用线程等待 Redis 结果；连接、类型和命令错误通过异常报告，不静默吞错。
 * 不支持在 Pipeline 或 Redis 事务回调中依赖同步返回值；结果未知不能解释为成功或未命中。
 * 多键操作不提供整体原子性或快照一致性；具体原子性与容量边界见方法说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/4
 */
public interface RedisDao {

    /**
     * 判断精确键是否存在，不读取键对应的值。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 键存在返回 true，不存在返回 false
     */
    boolean existsKey(Object key);

    /**
     * 删除精确键；请求失败时抛出异常，不能将结果未知解释为未删除。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 实际删除返回 true，键不存在返回 false
     */
    boolean delete(Object key);

    /**
     * 按业务命名空间模式清理键，扫描全部成功后才开始删除。
     *
     * <p>通配符之前必须有至少 4 个字符且包含冒号的固定前缀；模式长度不得超过 512。扫描超限不会删除已找到的键。删除阶段不具备整体原子性，失败时可能已有部分键被删除。</p>
     *
     * @param pattern 具有固定业务命名空间的 Redis glob 模式
     * @param count 每轮游标提示值；null 或非正值使用 1000，大于 1000 时按 1000 执行
     * @return 实际删除的键数
     * @throws IllegalStateException 扫描超出数量或时间预算时抛出
     */
    long deletePattern(Object pattern, Integer count);

    /**
     * 分批删除精确键，Cluster 模式逐键删除以支持跨槽。
     *
     * <p>整个批次不具备原子性，异常发生前可能已有部分键被删除。</p>
     *
     * @param keys 精确字符串键集合
     * @return 实际删除的键数，null 或空集合返回 0
     */
    long delete(Set keys);

    /**
     * 使用有界 SCAN 枚举匹配键，Cluster 模式遍历全部活动主节点。
     *
     * <p>数量和遍历时间受 peach.redis.tool.scan 配置约束，超限抛异常，不返回截断结果。COUNT 只是游标提示，不是总结果限制；默认及最大提示值为 1000。并发修改下不提供快照一致性，遍历预算不替代驱动网络超时。</p>
     *
     * @param matchKey Redis glob 匹配模式，不得为空白
     * @param count 每轮游标提示值；null 或非正值使用 1000，大于 1000 时按 1000 执行
     * @return 完整扫描后的去重结果；没有匹配时返回空集合
     * @throws IllegalStateException 扫描超出数量或时间预算时抛出
     */
    Set<Object> scan(String matchKey, Integer count);

    /**
     * 查询键的剩余存活时间，不修改过期设置。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 剩余毫秒数；-2 表示键不存在，-1 表示未设置过期时间
     */
    long ttl(String key);

    /**
     * 为已有键设置过期时间，可用于 String、Hash、List、Set 和 ZSet。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param timeout 至少 1 毫秒的正数过期时长
     * @return 设置成功返回 true，键不存在返回 false
     * @throws IllegalArgumentException 过期时长不足 1 毫秒时抛出
     */
    boolean expire(String key, Duration timeout);

    /**
     * 移除键的过期时间，使其保留至显式删除。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 移除成功返回 true；键不存在或原本没有过期时间返回 false
     */
    boolean persist(String key);

    /**
     * 写入 String 类型键，值使用应用统一序列化器编码。
     *
     * <p>覆盖已有值时会移除原 TTL；需要过期控制时使用带 TTL 的重载。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @return 命令正常完成返回 true；连接或命令失败抛异常
     */
    boolean vSet(Object key, Object value);

    /**
     * 写入 String 类型键，值使用应用统一序列化器编码。
     *
     * <p>值和过期时间由同一条 SET 命令写入；覆盖原值并重设 TTL。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @param expireTime 正数过期秒数
     * @return 命令正常完成返回 true；连接或命令失败抛异常
     * @throws IllegalArgumentException 过期时间为 null 或非正数时抛出
     */
    boolean vSet(Object key, Object value, Long expireTime);

    /**
     * 写入 String 类型键，值使用应用统一序列化器编码。
     *
     * <p>值和过期时间由同一条 SET 命令写入；覆盖原值并重设 TTL。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @param expire 正数过期时长，与值原子写入
     * @return 命令正常完成返回 true；连接或命令失败抛异常
     * @throws IllegalArgumentException 过期时间为 null 或非正数时抛出
     */
    boolean vSet(Object key, Object value, Duration expire);

    /**
     * 读取 String 类型键并使用应用统一序列化器解码。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 反序列化后的值，键不存在返回 null
     */
    Object vGet(Object key);

    /**
     * 原子增加整数计数器，键不存在时从 0 开始。
     *
     * <p>原值必须为 Redis 可识别的整数；超出整数范围或原值类型不符时传播命令异常。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param fromIndex 非负递增量，0 表示读取或初始化计数器
     * @return 递增后的计数值
     * @throws IllegalArgumentException 键为空白或增减量为负数时抛出
     */
    long increaseNum(String key, long fromIndex);

    /**
     * 原子减少整数计数器，键不存在时从 0 开始。
     *
     * <p>原值必须为 Redis 可识别的整数；不提供余额下限校验，超出整数范围或原值类型不符时传播命令异常。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param decIndex 非负递减量
     * @return 递减后的计数值，可以为负数
     * @throws IllegalArgumentException 键为空白或增减量为负数时抛出
     */
    long decreaseNum(String key, long decIndex);

    /**
     * 按输入顺序逐键读取，支持 Redis Cluster 跨槽。
     *
     * <p>不是单次 MGET，不保证整批结果来自同一个时间点；某次读取失败时直接抛异常，不返回部分结果。</p>
     *
     * @param keys 精确字符串键集合
     * @return 与输入等长的结果列表；不存在的键保留为 null，空输入返回空列表
     */
    List<Object> vMultiGet(List<String> keys);

    /**
     * 仅在键不存在时原子写入值并同时设置过期时间。
     *
     * <p>条件判断、写入和 TTL 属于同一条 SET NX 命令；这不是具有所有权校验、续租和重入语义的分布式锁。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @param ttl 至少 1 毫秒的正数过期时长，与条件写入原子执行
     * @return 写入成功返回 true，键已存在返回 false
     * @throws IllegalArgumentException 过期时长不足 1 毫秒时抛出
     */
    boolean vSetIfAbsent(String key, Object value, Duration ttl);

    /**
     * 仅在键存在时原子替换值并重设过期时间。
     *
     * <p>条件判断、写入和 TTL 属于同一条 SET XX 命令。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @param ttl 至少 1 毫秒的正数过期时长，与条件写入原子执行
     * @return 写入成功返回 true，键不存在返回 false
     * @throws IllegalArgumentException 过期时长不足 1 毫秒时抛出
     */
    boolean vSetIfPresent(String key, Object value, Duration ttl);

    /**
     * 原子读取 String 类型键并删除该键。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 删除前的值，键不存在返回 null
     */
    Object vGetAndDelete(String key);

    /**
     * 写入或覆盖单个 Hash 字段，不修改整个 Hash 键的 TTL。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param hashKey 字符串 Hash 字段名
     * @param value 非 null 的待写入或匹配值
     */
    void hmSet(Object key, Object hashKey, Object value);

    /**
     * 批量写入或覆盖 Hash 字段，未提供的已有字段保持不变。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param map 待写入的字段映射，字段名和值不得为 null
     */
    void hmSetAll(Object key, Map<Object, Object> map);

    /**
     * 读取 Hash 全部字段，仅适用于容量受控的小 Hash。
     *
     * <p>不受扫描预算保护；大 Hash 应使用 hmScan。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 字段和值的映射，键不存在返回空映射
     */
    Map<Object, Object> hmGet(Object key);

    /**
     * 读取 Hash 数据并使用应用统一序列化器解码。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param hashKey 字符串 Hash 字段名
     * @return 字段值，键或字段不存在返回 null
     */
    Object hmGet(Object key, Object hashKey);

    /**
     * 判断 Hash 中是否存在指定字段，不读取字段值。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param field 字符串 Hash 字段名
     * @return 字段存在返回 true，键或字段不存在返回 false
     */
    boolean hmHasKey(String key, String field);

    /**
     * 查询 Hash 字段数量，不读取字段内容。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 字段数量，键不存在返回 0
     */
    long hmSize(String key);

    /**
     * 按字段输入顺序读取一个 Hash 中的多个字段。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param fields 字符串字段名，批量读取时按输入顺序返回
     * @return 与输入等长的结果列表；不存在的字段保留为 null
     */
    List<Object> hmMultiGet(String key, List<String> fields);

    /**
     * 删除一个 Hash 中的多个字段，不删除其他字段。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param fields 字符串字段名，批量读取时按输入顺序返回
     * @return 实际删除的字段数量；空字段数组返回 0
     */
    long hmDelete(String key, String... fields);

    /**
     * 原子递增 Hash 数值字段，字段不存在时从 0 开始。
     *
     * <p>原值必须为 Redis 可识别的数字；整数重载受有符号 64 位范围约束，浮点重载要求增量有限。非数字原值会导致命令异常。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param field 字符串 Hash 字段名
     * @param delta 数值增量，可为负数
     * @return 递增后的数值
     */
    long hmIncrement(String key, String field, long delta);

    /**
     * 原子递增 Hash 数值字段，字段不存在时从 0 开始。
     *
     * <p>原值必须为 Redis 可识别的数字；整数重载受有符号 64 位范围约束，浮点重载要求增量有限。非数字原值会导致命令异常。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param field 字符串 Hash 字段名
     * @param delta 有限浮点增量，可为负数，不得为 NaN 或无穷
     * @return 递增后的数值
     */
    double hmIncrement(String key, String field, double delta);

    /**
     * 使用有界 HSCAN 读取一个 Hash 的全部字段。
     *
     * <p>受全局扫描数量和时间预算约束，超限抛异常，不返回部分结果；方法内部关闭游标，不提供并发修改下的快照一致性。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param count 每轮游标提示值；null 或非正值使用 1000，大于 1000 时按 1000 执行
     * @return 字段和值的映射；键不存在返回空映射
     * @throws IllegalStateException 扫描超出数量或时间预算时抛出
     */
    Map<Object, Object> hmScan(String key, Integer count);

    /**
     * 查询 List 长度，不读取元素内容。
     *
     * @param k 最终字符串键，Object 参数仅保留历史签名兼容
     * @return 列表长度，键不存在返回 0
     */
    long lSize(Object k);

    /**
     * 按索引读取 List 元素，不移除该元素。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param index 元素索引，0 为首位，-1 为末位
     * @return 指定位置的值；键不存在或索引越界返回 null
     */
    Object lindex(Object key, long index);

    /**
     * 从 List 左侧插入一个元素，键不存在时创建列表。
     *
     * @param k 最终字符串键，Object 参数仅保留历史签名兼容
     * @param v 非 null 的待写入或匹配值
     */
    void lLeftPush(Object k, Object v);

    /**
     * 将一批元素依次从 List 左侧插入。
     *
     * <p>输入 [a,b] 插入空列表后的顺序是 [b,a]；元素使用统一值序列化器编码。</p>
     *
     * @param k 最终字符串键，Object 参数仅保留历史签名兼容
     * @param lst 非空元素列表，不得包含 null
     */
    void lLeftPushAll(Object k, List<Object> lst);

    /**
     * 从 List 右侧插入一个元素，键不存在时创建列表。
     *
     * @param k 最终字符串键，Object 参数仅保留历史签名兼容
     * @param v 非 null 的待写入或匹配值
     */
    void lRightPush(Object k, Object v);

    /**
     * 将一批元素依次从 List 右侧插入。
     *
     * <p>输入 [a,b] 插入空列表后的顺序是 [a,b]；元素使用统一值序列化器编码。</p>
     *
     * @param k 最终字符串键，Object 参数仅保留历史签名兼容
     * @param lst 非空元素列表，不得包含 null
     */
    void lRightPushAll(Object k, List<Object> lst);

    /**
     * 移除并返回 List 左侧元素，此方法不等待后续入队。
     *
     * @param k 最终字符串键，Object 参数仅保留历史签名兼容
     * @return 被移除的元素，键不存在或列表为空返回 null
     */
    Object lLeftPop(Object k);

    /**
     * 移除并返回 List 右侧元素，此方法不等待后续入队。
     *
     * @param k 最终字符串键，Object 参数仅保留历史签名兼容
     * @return 被移除的元素，键不存在或列表为空返回 null
     */
    Object lRightPop(Object k);

    /**
     * 按值匹配并移除 List 元素，匹配基于序列化后的值。
     *
     * <p>count&gt;0 从左侧移除至多 count 个，count&lt;0 从右侧移除至多绝对值个，
     * count=0 移除全部匹配值；整次移除由单条 LREM 完成。</p>
     *
     * @param k 最终字符串键，Object 参数仅保留历史签名兼容
     * @param count 匹配数量，正数从左匹配，负数从右匹配，0 为全部
     * @param v 非 null 的待写入或匹配值
     * @return 实际移除的元素数量，键或匹配值不存在返回 0
     */
    long lRemove(Object k, long count, Object v);

    /**
     * 读取 List 的索引闭区间，不修改列表。
     *
     * <p>索引支持负数，0,-1 可读取整个列表；调用方应限制读取范围，避免一次加载大列表。</p>
     *
     * @param k 最终字符串键，Object 参数仅保留历史签名兼容
     * @param start 起始索引，包含，支持负索引
     * @param end 结束索引，包含，支持负索引
     * @return 按列表顺序排列的区间元素；键不存在或区间为空返回空列表
     */
    List<Object> lRange(Object k, long start, long end);

    /**
     * 替换 List 已有索引位置的元素，不改变列表长度。
     *
     * <p>键不存在或索引越界时传播 Redis 命令异常。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param index 元素索引，0 为首位，-1 为末位
     * @param value 非 null 的待写入或匹配值
     */
    void lSet(String key, long index, Object value);

    /**
     * 仅保留 List 索引闭区间内的元素，区间外元素会被删除。
     *
     * <p>支持负索引；不与其他读写组成原子业务操作。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param start 起始索引，包含，支持负索引
     * @param end 结束索引，包含，支持负索引
     */
    void lTrim(String key, long start, long end);

    /**
     * 向 Set 添加一个成员，重复成员不会新增。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     */
    void sAdd(Object key, Object value);

    /**
     * 读取 Set 的全部成员，仅适用于容量受控的小集合。
     *
     * <p>不受扫描预算保护；大集合应使用 sScan。结果不保证成员顺序。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 反序列化后的成员集合，键不存在返回空集合
     */
    Set<Object> sMembers(Object key);

    /**
     * 随机读取一个 Set 成员，不移除该成员。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 随机成员，键不存在或集合为空返回 null
     */
    Object sRandomMember(String key);

    /**
     * 随机移除并返回一个 Set 成员。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 被移除的成员，键不存在或集合为空返回 null
     */
    Object sPop(String key);

    /**
     * 移除 Set 中的指定成员，不存在的成员不影响其他成员。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param values 待处理成员，不得包含 null
     */
    void sRemove(String key, Object... values);

    /**
     * 原子向 Set 添加多个成员，重复成员不会新增。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param values 待处理成员，不得包含 null
     * @return 实际新增的成员数量，空数组返回 0
     */
    long sAddAll(String key, Object... values);

    /**
     * 判断 Set 是否包含指定成员，匹配基于序列化后的值。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @return 成员存在返回 true，键或成员不存在返回 false
     */
    boolean sIsMember(String key, Object value);

    /**
     * 查询 Set 成员数量，不读取成员内容。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 成员数量，键不存在返回 0
     */
    long sSize(String key);

    /**
     * 计算两个 Set 的交集，不修改输入集合。
     *
     * <p>客户端逐键执行有界 sScan 后计算，支持 Cluster 跨槽；每个输入独立受扫描预算约束，不保证两个集合处于同一快照。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param otherKey 第二个 Set 的最终字符串键
     * @return 两个集合共有的成员，无交集时返回空集合
     */
    Set<Object> sIntersect(String key, String otherKey);

    /**
     * 计算两个 Set 的并集，不修改输入集合。
     *
     * <p>客户端逐键执行有界 sScan 后计算，支持 Cluster 跨槽；每个输入独立受扫描预算约束，不保证两个集合处于同一快照。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param otherKey 第二个 Set 的最终字符串键
     * @return 两个集合合并后的去重成员
     */
    Set<Object> sUnion(String key, String otherKey);

    /**
     * 计算第一个 Set 相对第二个 Set 的差集，不修改输入集合。
     *
     * <p>客户端逐键执行有界 sScan 后计算，支持 Cluster 跨槽；每个输入独立受扫描预算约束，不保证两个集合处于同一快照。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param otherKey 第二个 Set 的最终字符串键
     * @return 仅在第一个集合中存在的成员
     */
    Set<Object> sDifference(String key, String otherKey);

    /**
     * 使用有界 SSCAN 读取一个 Set 的全部成员，保留反序列化类型。
     *
     * <p>受全局扫描数量和时间预算约束，超限抛异常，不返回部分结果；内部关闭游标，结果不保证顺序或快照一致性。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param count 每轮游标提示值；null 或非正值使用 1000，大于 1000 时按 1000 执行
     * @return 去重后的成员集合，键不存在返回空集合
     * @throws IllegalStateException 扫描超出数量或时间预算时抛出
     */
    Set<Object> sScan(String key, Integer count);

    /**
     * 向 ZSet 添加成员或更新已有成员分值，不修改键的 TTL。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @param scoure 成员分值，沿用历史参数名称
     */
    void zAdd(Object key, Object value, double scoure);

    /**
     * 原子批量添加 ZSet 成员或更新已有成员分值。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param scores 成员及有限分值的映射，成员和分值不得为 null
     * @return 实际新增的成员数量；已有成员仅更新分值，空映射返回 0
     * @throws IllegalArgumentException 增量或分值不是有限数值时抛出
     */
    long zAddAll(String key, Map<Object, Double> scores);

    /**
     * 删除 ZSet 指定成员，不影响其他成员的分值。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param values 待处理成员，不得包含 null
     * @return 实际删除的成员数量，空数组返回 0
     */
    long zRemove(String key, Object... values);

    /**
     * 读取 ZSet 成员的分值。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @return 成员分值，键或成员不存在返回 null
     */
    Double zScore(String key, Object value);

    /**
     * 原子增加 ZSet 成员分值，成员不存在时以增量作为初始分值。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @param delta 有限浮点增量，可为负数，不得为 NaN 或无穷
     * @return 更新后的成员分值
     * @throws IllegalArgumentException 增量或分值不是有限数值时抛出
     */
    double zIncrementScore(String key, Object value, double delta);

    /**
     * 查询 ZSet 成员按分值升序排列时的排名。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @return 从 0 开始的排名，键或成员不存在返回 null
     */
    Long zRank(String key, Object value);

    /**
     * 查询 ZSet 成员按分值降序排列时的排名。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param value 非 null 的待写入或匹配值
     * @return 从 0 开始的排名，键或成员不存在返回 null
     */
    Long zReverseRank(String key, Object value);

    /**
     * 按排名闭区间升序读取 ZSet 成员。
     *
     * <p>排名从 0 开始，支持负索引；调用方应限制区间大小。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param start 起始索引，包含，支持负索引
     * @param end 结束索引，包含，支持负索引
     * @return 按排名升序遍历的成员集合；键不存在或区间为空返回空集合
     */
    Set<Object> zRange(String key, long start, long end);

    /**
     * 按排名闭区间降序读取 ZSet 成员。
     *
     * <p>排名从 0 开始，支持负索引；调用方应限制区间大小。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param start 起始索引，包含，支持负索引
     * @param end 结束索引，包含，支持负索引
     * @return 按排名降序遍历的成员集合；键不存在或区间为空返回空集合
     */
    Set<Object> zReverseRange(String key, long start, long end);

    /**
     * 按排名闭区间升序读取 ZSet 成员及其分值。
     *
     * <p>排名从 0 开始，支持负索引；调用方应限制区间大小。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param start 起始索引，包含，支持负索引
     * @param end 结束索引，包含，支持负索引
     * @return 按排名升序遍历的成员与分值集合，键不存在或区间为空返回空集合
     */
    Set<ZSetOperations.TypedTuple<Object>> zRangeWithScores(String key, long start, long end);

    /**
     * 按分值闭区间升序分页读取 ZSet 成员。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param min 分值下界，包含，可为负无穷，不得为 NaN 或大于 max
     * @param max 分值上界，包含，可为正无穷，不得为 NaN 或小于 min
     * @param offset 跳过的成员数量，不得为负数
     * @param count 当前页最多返回的成员数量，必须为正数
     * @return 当前页成员，键不存在或页内无成员返回空集合
     * @throws IllegalArgumentException 分值区间无效，或分页参数无效时抛出
     */
    Set<Object> zRangeByScore(String key, double min, double max, long offset, long count);

    /**
     * 按分值闭区间降序分页读取 ZSet 成员。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param min 分值下界，包含，可为负无穷，不得为 NaN 或大于 max
     * @param max 分值上界，包含，可为正无穷，不得为 NaN 或小于 min
     * @param offset 跳过的成员数量，不得为负数
     * @param count 当前页最多返回的成员数量，必须为正数
     * @return 当前页成员，键不存在或页内无成员返回空集合
     * @throws IllegalArgumentException 分值区间无效，或分页参数无效时抛出
     */
    Set<Object> zReverseRangeByScore(String key, double min, double max, long offset, long count);

    /**
     * 查询 ZSet 全部成员数量，不读取成员或分值。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @return 成员数量，键不存在返回 0
     */
    long zSize(String key);

    /**
     * 统计 ZSet 分值闭区间内的成员数量。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param min 分值下界，包含，可为负无穷，不得为 NaN 或大于 max
     * @param max 分值上界，包含，可为正无穷，不得为 NaN 或小于 min
     * @return 满足区间条件的成员数量，键不存在返回 0
     * @throws IllegalArgumentException 分值区间无效，或分页参数无效时抛出
     */
    long zCount(String key, double min, double max);

    /**
     * 删除 ZSet 排名闭区间内的成员。
     *
     * <p>排名从 0 开始，支持负索引；调用方应限制区间大小。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param start 起始索引，包含，支持负索引
     * @param end 结束索引，包含，支持负索引
     * @return 实际删除的成员数量
     */
    long zRemoveRange(String key, long start, long end);

    /**
     * 删除 ZSet 分值闭区间内的成员。
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param min 分值下界，包含，可为负无穷，不得为 NaN 或大于 max
     * @param max 分值上界，包含，可为正无穷，不得为 NaN 或小于 min
     * @return 实际删除的成员数量
     * @throws IllegalArgumentException 分值区间无效，或分页参数无效时抛出
     */
    long zRemoveRangeByScore(String key, double min, double max);

    /**
     * 使用有界 ZSCAN 读取一个 ZSet 的全部成员及分值。
     *
     * <p>受全局扫描数量和时间预算约束，超限抛异常，不返回部分结果；内部关闭游标，不保证排名顺序或快照一致性。</p>
     *
     * @param key 由业务 KeyDefinition 和 KeyBuilder 生成的最终字符串键
     * @param count 每轮游标提示值；null 或非正值使用 1000，大于 1000 时按 1000 执行
     * @return 成员和分值的映射，键不存在返回空映射
     * @throws IllegalStateException 扫描超出数量或时间预算时抛出
     */
    Map<Object, Double> zScan(String key, Integer count);

    /**
     * 向 Redis Pub/Sub 通道发布消息，内容使用应用模板的值序列化器编码。
     *
     * <p>没有订阅者也属于正常发布；方法不承诺消费者收到、处理或持久化消息，不适合作为可靠消息队列。</p>
     *
     * @param topic 非空白发布通道，业务负责命名空间隔离
     * @param message 非 null 的消息对象，不应包含凭据或其他敏感内容
     */
    void convertAndSend(String topic, Object message);
}
