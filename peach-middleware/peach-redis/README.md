# peach-redis

[English](README.en-US.md) | 中文

`peach-redis` 通过独立 `peach-redis-starter` 提供 Jedis / Spring Data Redis 连接底座，Tool、多级缓存和 Stream 按能力选择 Starter。旧混合客户端模块 `peach-redis-common` 已从仓库源码、Maven reactor 和 BOM 删除，不再作为可构建的兼容工件；原使用方必须迁移 Starter，不能继续依赖旧坐标。

## 组件定位与版本

**面向谁：** 需要 Redis 数据结构操作、应用级 L1 + Redis L2 缓存或 Redis Stream 消息能力的 Spring Boot 业务服务。这里的 `peach-redis` 是 Maven 聚合模块，不是将全部能力打包的单个依赖。按需要选用对应 Starter，避免将 Stream、缓存和通用 DAO 一并接入。

**当前基线：** Java 21 / Spring Boot 3.5.4。独立 Redis 底座使用 Jedis，独立 Redisson 使用 `peach-redisson-starter`；不再发布旧 `peach-redis-common`。历史序列化配置已作为测试夹具保留，不等于旧发行版二进制滚动兼容验收。以下矩阵反映当前源码能力，项目收尾见 [Issue #18](https://github.com/Ryan-Guizhou/peach-cloud/issues/18)。

**截至本轮的代码验收：** 独立客户端、Sa-Token 认证、Redis Tool、MultiCache、Stream 已通过对应的 Redis 7.2 专项验证；原位 Sentinel 故障恢复（[#70](https://github.com/Ryan-Guizhou/peach-cloud/pull/70)）、Cluster 主节点切换与新客户端恢复（[#74](https://github.com/Ryan-Guizhou/peach-cloud/pull/74)）、旧/新源码序列化字节兼容（[#66](https://github.com/Ryan-Guizhou/peach-cloud/pull/66)）、Stream 死信字段/只读对账（[#90](https://github.com/Ryan-Guizhou/peach-cloud/pull/90)、[#94](https://github.com/Ryan-Guizhou/peach-cloud/pull/94)）均已有合并记录。**尚未完成的生产验收**是实际旧发行版 SaSession fixture 与混合发布、目标环境 Nacos/ACL/TLS 轮换及真实业务故障注入。完整证据与回滚门槛见[迁移验收手册](../../docs/redis-redisson/acceptance-and-rollout.md)。

**MultiCache L1 竞争边界：** 收到 Pub/Sub 失效通知后会推进本地失效版本号；在途 Redis GET/PTTL 只在版本未变化时回填 Caffeine，避免通知到达后被旧读取重新污染 L1。Redis Pub/Sub 仍是最终一致的失效通道，网络中断期间依赖重新订阅时全量清空 L1，不能替代业务数据版本或数据库事务。

**动态缓存数量上限：** `peach.multicache.max-cache-instances` 默认为 `256`，限定一个 `MultiCacheManager` 可创建的 Caffeine/L1 缓存实例总数；新缓存名超过上限会显式报错，而不是无限占用堆内存。已创建的动态缓存也计入 `getCacheNames()`，可被 `clearAll()` 遍历。请不要直接使用无限变化的用户输入作为缓存名称；缓存 Key 应放在固定缓存名之下。

## ACL、Sentinel 与 Cluster 配置契约（Issue #49）

独立 `peach-redis-starter` 同时支持数据节点 ACL 和独立 Sentinel ACL；两类认证信息不应混用。下列键在未配置时保持兼容默认值：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.redis.username` | 空 | Redis 数据节点用户名，Standalone / Sentinel / Cluster 均适用 |
| `peach.redis.password` | 空 | Redis 数据节点密码 |
| `peach.redis.sentinel-username` | 空 | Sentinel 自身的 ACL 用户名，可与数据节点不同 |
| `peach.redis.sentinel-password` | 空 | Sentinel 自身的密码 |
| `peach.redis.ssl-enabled` | `false` | Jedis SSL/TLS 开关；与 Redisson 的 `ssl-enable` 独立 |
| `peach.redis.jedis.max-total` | `128` | Jedis 连接池最大连接数 |

Redis Cluster 只支持数据库 `0`，非法值启动时直接失败。两份 Nacos 模板使用相同配置键；密码应在安全配置源维护，不能提交真实凭据。应用自定义 `RedisConnectionFactory` 仍优先于默认 Jedis 工厂。具体多拓扑执行方式见下方 Issue #47 验收章节。

## 完整能力矩阵

| 模块 / Starter | 核心入口 | 能做什么 | 典型场景 | 当前限制 / 注意事项 |
| --- | --- | --- | --- | --- |
| 连接底座 / `peach-redis-starter`（推荐） | `RedisConnectionFactory`、`RedisTemplate`、`StringRedisTemplate` | 独立 Jedis、Standalone/Sentinel/Cluster、TLS、ACL、连接池、自定义工厂退让 | Sa-Token / Tool / 缓存 / Stream | 不创建 `RedissonClient`；Cluster 仅数据库 0；保留旧序列化格式，旧会话滚动兼容待验证 |
| `peach-redis-tool-starter` | `RedisDao` | String、TTL、Hash、List、Set、ZSet、Pub/Sub 和批量删除 | 会话辅助数据、计数、业务 KV、标签 | `scan()` 支持单机/Cluster；默认最多返回 10,000 个唯一 key、最长 10 秒，超限抛异常；`count` 仅为批次提示，跨槽删除逐键执行 |
| `peach-redis-multicache-starter` | `MultiCacheManager`、`MultiCacheManagerService`、Spring Cache | Caffeine L1 + Redis L2、TTL 抖动、CacheManager/注解接入、Pub/Sub 失效广播 | 商品详情、相对稳定的配置/字典缓存 | Pub/Sub 是尽力失效而非强一致；`clear()` 使用有界模式删除；扫描超限直接失败并清理本地 L1，不能承诺 Redis L2 已全部删除；跨实例无强一致保证 |
| `peach-redis-stream-starter` | `RedisStreamPushHandler`、`RedisStreamHandler`、`MessageConsumer` | Stream 生产、group/broadcast 消费、消费者回调 | 轻量业务消息、事件处理 | 消费组仅在业务成功后显式 ACK；失败保留 PEL；可选定时认领、指数退避及 DLQ（默认关闭，仍需故障注入） |

### 如何选择

- **批量查找与清理：** `RedisDao#scan` 默认最多收集 10,000 个唯一键且最多执行 10 秒；超限明确报错、不返回截断结果。`deletePattern` 拒绝 `*` 等无业务前缀的通配模式，全部扫描成功才分批删除。`COUNT` 仍只是 Redis 遍历提示，操作不是一致快照；大键空间需按业务 namespace 拆分处理。
- **仅缓存键值、结构化数据：** 使用 Tool。原生 `RedisTemplate` 可直接满足的场景无需再次封装 DAO。
- **Caffeine L1 选项：** 默认强引用，显式 `WEAK` 会改变缓存键的引用匹配行为；`putIfAbsent` 使用 Redis 原子 SET NX，而跨节点失效仍依赖尽力而为的 Pub/Sub。
- **需要本地缓存加速：** 使用 MultiCache，并接受短暂陈旧、设置合理 TTL；安全相关实时授权不要仅依赖本地缓存。
- **需要消息消费：** 使用 Stream；当前版本应自行保证业务幂等及失败恢复，不能视为可靠投递的完整替代方案。
- **需要锁、布隆过滤器、延迟队列、防重复：** 转到 [peach-redission](../peach-redission/README.md)，不要把该能力误归入 Redis Tool。
- **Gateway：** 使用独立响应式 Lettuce；不要通过 Redis Tool 为 WebFlux 事件循环引入阻塞调用。

### 集合扫描与缓存清理兼容说明（Issue #97）

- 已废弃的 `RedisDao#hscan/sscan/zscan(matchKey, count)` 使用第一个参数作为实际 **Hash/Set/ZSet Redis key**，而不是字段 glob；它们读取全部成员，`count` 只是批次提示，并受下面的 `max-results` / `timeout-millis` 限制，超限抛异常。之前依赖硬编码的 `field`、`setValue`、`zSetValue` 键的调用方须显式传入实际键。
- `MultiCache.clear()` 通过构造器注入的 `RedisDao` 执行有界命名空间删除，不再依赖全局 Hutool `SpringUtil`；扫描失败仍清理本地 L1，不能视为远端 L2 成功删除。
- `vSetUpdate` 现在按 Redis `EXPIRE` 返回值报告结果；键不存在时返回 `false`，不再误报成功。

### Redis Tool 有界扫描配置

```yaml
peach:
  redis:
    tool:
      scan:
        max-results: 10000
        timeout-millis: 10000
```

这些限制默认生效，分别允许 1–100000 个唯一 key、1–60000 毫秒。历史 `scan(matchKey, count)`、`keys(pattern)`、`deletePattern(pattern, count)` 签名不变。超过上限会失败，不静默截断结果；`MultiCache.clear()` 在二级删除失败时仍清空本实例 L1，但不会声称其他节点 L2 已清空。

### 最简依赖（当前版本）

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-tool-starter</artifactId>
    <version>${revision}</version>
</dependency>
```

`revision` 来自本仓库 Maven reactor；仓库外使用已发布的实际版本。Tool / MultiCache / Stream Starter 传递 `peach-redis-starter`，旧 `peach-redis-common` 不再构建；对旧环境升级仍需核对 Nacos 配置、Session 与存量 Redis 数据。

### 最简配置（当前项目约定）

```yaml
peach:
  redis:
    mode: standalone
    host: "${REDIS_HOST:127.0.0.1:6379}"
    password: "${REDIS_PASSWORD:}"
    database: 0
```

此示例使用开发环境占位信息；生产环境凭据应来自安全配置源。当前拓扑字段是 `peach.redis.*` 而非 `spring.data.redis.*`。多级缓存需额外配置 `peach.multicache.*`，Stream 的启用项是 **`peach.redis.stream.enable`**（不是 `enabled`）。

### 代码示例（Tool）

```java
@Service
@RequiredArgsConstructor
public class ProfileCacheService {
    private final RedisDao redisDao;

    public void cache(String userId, String value) {
        redisDao.vSet("profile:" + userId, value, java.time.Duration.ofMinutes(5));
    }

    public Object find(String userId) {
        return redisDao.vGet("profile:" + userId);
    }
}
```

以上通过已有 `RedisDao` API 演示。建议生产中统一 key 命名、TTL 和故障策略，且避免在 key 或日志中写入个人敏感信息。

### 故障与兼容约定

| 情况 | 处理原则 |
| --- | --- |
| Redis 不可达 | 缓存可按业务回源；凭证、鉴权、验证码等关键路径不应放行 |
| 多级缓存失效订阅掉线 | 本地值可能陈旧，限制 L1 TTL，重连清理和跨实例一致性仍待完善 |
| Stream 处理异常 | group 模式失败消息留在 PEL；可选的 XPENDING/XCLAIM 定时恢复、退避及 DLQ 默认关闭，启用前必须完成业务持久幂等和故障注入验收 |
| 更换 Jackson/客户端 | 先验证旧 Redis JSON 与存量 key 可读，不可直接变更会话协议 |


## 业务依赖迁移进展（Issue #41）

- **Sa-Token**：autoconfigure 通过 `RedisConnectionFactory` 接入，Starter 传递 `peach-redis-starter`；旧混合工件已删除，现有 Sa-Token JSON/key 约定保持不变。
- **Captcha**：autoconfigure 只声明可选的 Spring Data Redis、Hutool 编译依赖，内存缓存实现仍可独立使用。
- **Auth / Setting / Message**：业务模块改为独立 Redis Starter；Auth 启动模块移除能由 Starter 传递的旧 Jedis、Redisson 和 Spring Data Redis 直连依赖。
- **目标环境验收**：存量 Redis key、真实旧发行版 Session 登录/退出、网关 Lettuce 隔离、Nacos 配置和 Sentinel/Cluster/TLS 发布需在目标环境复核；移除 Maven 模块本身不代表这些生产操作已执行。

## 旧客户端依赖收敛（Issue #43）

Redis Tool / MultiCache / Stream 的 autoconfigure 与 Quickstart POM 不再依赖旧 `peach-redis-common`；本轮连同 BOM、reactor 和遗留 `RedisConfig` 源码一起删除。`RedisDaoImpl` 使用模块内 Cluster 常量，Redisson 能力也不依赖旧模块。真实 Redis 7.2 基础验证不能替代生产环境的跨版本数据及会话滚动验收。

## 结构

| 能力 | Autoconfigure | Starter | Quickstart |
| --- | --- | --- | --- |
| Tool | `peach-redis-tool-autoconfigure` | `peach-redis-tool-starter` | `peach-redis-tool-quickstart` |
| MultiCache | `peach-redis-multicache-autoconfigure` | `peach-redis-multicache-starter` | `peach-redis-multicache-quickstart` |
| Stream | `peach-redis-stream-autoconfigure` | `peach-redis-stream-starter` | `peach-redis-stream-quickstart` |

```mermaid
flowchart LR
    App[业务服务] --> Tool[Tool Starter]
    App --> Cache[MultiCache Starter]
    App --> Stream[Stream Starter]
    Tool --> Redis[(Redis)]
    Cache --> Redis
    Stream --> Redis
```

## 选择

- 通用 Redis 访问：`peach-redis-tool-starter`。
- 本地 + Redis 多级缓存：`peach-redis-multicache-starter`。
- Redis Stream 消费/生产：`peach-redis-stream-starter`。

## Quick Start

各能力均提供无 Web 端口的 quickstart（`spring.main.web-application-type=none`），只需对应 Starter 的依赖闭包。传统 Testcontainers 测试在缺少 Docker 环境时可能跳过，另有专用 Redis 7.2 服务容器 CI 实际运行 Tool 和客户端验收。本地演示前提供开发 Redis，或直接跑 IT。

### Tool

- 模块：[`peach-redis-tool-quickstart`](./peach-redis-tool-quickstart/)
- 能力样例（注入 `RedisDao`）：
  - **String + TTL**：会话 token 写入 / 读取 / 存在判断 / 删除
  - **Hash**：用户资料批量写入、按字段读取与字段删除
  - **Set**：用户标签添加 / 列表 / 移除
- 启动后 `RedisToolDemoRunner` 依次执行；关闭演示：`quickstart.redis-tool.demo.enabled=false`
- 运行：`mvn -pl peach-middleware/peach-redis/peach-redis-tool-quickstart -am spring-boot:run`
- 测试：`mvn -pl peach-middleware/peach-redis/peach-redis-tool-quickstart -am test`

### MultiCache

- 模块：[`peach-redis-multicache-quickstart`](./peach-redis-multicache-quickstart/)
- 能力样例（商品详情场景，两种接入方式）：
  - **CacheManager**：注入自动装配的 `CacheManager`，显式 `put/get`、未命中回源、`evict` 后再次回源
  - **分层证明**：仅清 L1(Caffeine) 后仍命中 L2(Redis) 并回填本地，且不回源
  - **注解**：`@EnableCaching` + `@Cacheable` / `@CacheEvict`，同样走该 `CacheManager`
- 启动后 `MulticacheDemoRunner` 依次跑上述演示；测试用 `InMemoryProductStore#loadCount` 与 `clearLocal` 证明命中路径
- 最小配置：`peach.multicache.enabled=true`、`peach.multicache.cache-names`（`product-manager` / `product-annotation`），以及 `peach.redis.*`
- 运行：`mvn -pl peach-middleware/peach-redis/peach-redis-multicache-quickstart -am spring-boot:run`
- 测试：`mvn -pl peach-middleware/peach-redis/peach-redis-multicache-quickstart -am test`
- 关闭启动演示：`quickstart.multicache.demo.enabled=false`

### Stream

### Pending 恢复与 DLQ（Issue #37）

**真实 Redis 7.2 恢复验收（Issue #51）：** 已在 Redis 服务容器新增 `RedisStreamRecoveryServerIntegrationTest`：验证 abandoned consumer 的 PEL 经 `XPENDING/XCLAIM` 被认领、回调成功后 `XACK`；业务异常仍保留 PEL；重试次数耗尽时先把 `sourceId/sourceStream/sourceGroup` 写入 DLQ，再确认源消息。CI 会检查 4 项轻量真实 Redis 测试执行、无跳过/失败；包含新死信源消息被裁剪后仍能原样重放、历史死信无源记录时拒绝重放。恢复默认关闭，此测试不证明 Redis 主从切换时的幂等与跨键原子性。

- **默认关闭**：设置 `peach.redis.stream.recovery.enabled=true` 开启；仅适用于 Consumer Group。启用之前必须让业务消费者按消息 ID 或业务主键实现持久幂等。广播模式不支持恢复。
- **恢复算法**：单线程定时以 `XPENDING` 分页读取，每次不超过 `batch-size`，然后以 `XCLAIM` 认领超过动态空闲阈值的记录；重试间隔按投递次数指数递增（`min-idle` 到 `max-retry-delay`）。业务处理成功后 XACK。
- **死信路径**：当投递次数达到 `max-attempts`，把原 RecordId、来源、投递次数与载荷写入独立 DLQ Stream；写入确认后才 XACK 源记录。默认 DLQ 名为 `源流名:dlq`。DLQ 与源流非跨键原子提交，崩溃时可能重复写死信，必须按 `sourceId` 去重。
- **运维重放、统计与对账（Issue #87 / #93）**：可注入 `RedisStreamPendingRecovery` 使用 `backlog()` 获取 PEL/DLQ 数量，调用 `replayDeadLetter(dlqRecordId)` 将原 Stream 仍存在的消息字段重发为新 RecordId。先核对 DLQ 的 `sourceStream/sourceGroup/sourceId`。新死信在现有字段之外额外保存 `originalField:` 编码的原始业务字段，源记录被裁剪后仍可恢复；历史死信仍需从源 Stream 精确读取，已裁剪则拒绝。原死信保留供审计。重复手动执行可能重复发布，必须有业务持久幂等。
- **跨键 DLQ/XACK 故障窗口（Issue #93）**：调用 `deadLetterTransferState(dlqRecordId)` 会校验死信来源 Stream/Group/ID，仅按精确 ID 读取并返回源 ID、是否仍在 PEL、源记录是否存在；**不会**确认、删除、重放或推断业务是否完成。人工处理前须暂停同组所有实例消费与恢复，并根据业务真值对账。
- **验收边界**：仍为至少一次，故障注入、Redis 主从切换/Cluster、告警接入尚需目标环境验收；不可宣称 exactly-once 或强一致恢复。客户端重启不会清空 PEL。

```yaml
peach:
  redis:
    stream:
      enable: true
      consumer-type: group
      recovery:
        enabled: true
        batch-size: 32
        max-attempts: 5
        min-idle: 60s
        max-retry-delay: 15m
        interval: 30s
        # dead-letter-stream: order-status:dlq
```


**确认与恢复边界**：Consumer Group 使用手动 XACK：业务回调成功才确认，失败保留 PEL。每个服务实例的消费者名称带随机后缀；线程池由 Spring 管理。可选 `XPENDING`/`XCLAIM` 自动认领、退避与 DLQ 已实现但默认关闭；跨键 DLQ 写入与源流 XACK 非原子操作，处理关键事件必须独立持久化幂等。


- 模块：[`peach-redis-stream-quickstart`](./peach-redis-stream-quickstart/)
- 能力样例（订单状态流）：
  - **Push + Consume**：`RedisStreamPushHandler#push` 拿 `RecordId`，业务实现 `MessageConsumer` 消费确认
  - **幂等**：同一 `RecordId` 回放时业务侧去重（accepted=1 / duplicate=1）
- 启动后 `RedisStreamDemoRunner` 执行；关闭演示：`quickstart.stream.demo.enabled=false`
- 最小配置：`peach.redis.stream.enable=true`（`consumer-type=group`），以及 `peach.redis.*`
- 运行：`mvn -pl peach-middleware/peach-redis/peach-redis-stream-quickstart -am spring-boot:run`
- 测试：`mvn -pl peach-middleware/peach-redis/peach-redis-stream-quickstart -am test`

`peach.redis.host` 格式为 `host:port`。Quickstart 统一默认：`password` 默认为空（通过 `PEACH_REDIS_PASSWORD` 覆盖）、`database=1`。

## Redis-only 底座（新增）

本仓库提供 `peach-redis-autoconfigure`、`peach-redis-starter`、`peach-redis-quickstart`；独立 Jedis 底座不创建 `RedissonClient`，Tool / MultiCache / Stream 能力通过各自 Starter 接入。旧 `peach-redis-common` 已删除，旧业务须明确迁移到相应 Starter，不得依赖原聚合 jar。

| 能力 | 接入入口 | 重要约束 |
| --- | --- | --- |
| Redis-only / `peach-redis-starter` | `RedisConnectionFactory` / `RedisTemplate` / `StringRedisTemplate` | 默认 Jedis；已有用户工厂优先；JSON value 使用旧 Jackson2JsonRedisSerializer 协议 |
| Tool / `peach-redis-tool-starter` | `RedisDao` | 独立依赖新底座，SCAN 大键空间需低频执行 |
| MultiCache / `peach-redis-multicache-starter` | `MultiCacheManagerService` | 额外依赖 Tool、Caffeine 与 Hutool，缓存同步为尽力而为 |
| Stream / `peach-redis-stream-starter` | `RedisStreamHandler` | 使用通用 `RedisConnectionFactory`；业务成功后 ACK、失败保留 PEL；可选 Pending 认领、退避与 DLQ 默认关闭 |

**最小依赖**（项目父 POM 管理版本）：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-starter</artifactId>
    <version>${revision}</version>
</dependency>
```

**最小配置**：沿用 `peach.redis.mode / host / password / database`；可通过 `peach.redis.enabled=false` 关闭新自动装配、`peach.redis.ssl-enabled=true` 开启 TLS、`peach.redis.jedis.max-total` 调整连接预算。已有 `RedisConnectionFactory` 优先，默认池 `max-total=128`。

**最短路径**：仓库根目录运行 `mvn -pl peach-middleware/peach-redis/peach-redis-quickstart -am spring-boot:run`；需 Docker 的真实集成测试执行对应 Maven test 和专用 Redis CI。Quickstart 不需要 Redisson；Sa-Token、Auth/Setting/Message 已接入独立底座。旧 `RedisConfig` 不再提供，真实旧发行版会话仍须单独做生产滚动验证。

## 真实 Redis 多拓扑集成验收（Issue #47）

当前仓库提供独立的 Redis 7.2 CI 矩阵，在 **Sentinel（三 Sentinel/一主两副本）**、**Cluster（三主三副本）** 和 **TLS 单节点** 三种环境执行 Redis-only、Redisson-only、Redis Tool 的真实集成测试。使用独立的 `peach.redis.*` 自动装配：检查真实 KV/TTL、Bean 隔离、跨主节点 SCAN 和 Cluster 跨槽逐键删除；Surefire XML 强制每个测试套件恰好运行一项且无跳过或失败。TLS 使用临时证书及 Java 显式信任库，不关闭证书校验。

仓库根目录在带 Docker host network 的 Linux 环境可复现（需要 Java 21、Maven、openssl、keytool）：

```bash
bash scripts/ci/redis-topology-integration.sh sentinel
bash scripts/ci/redis-topology-integration.sh cluster
bash scripts/ci/redis-topology-integration.sh tls
```

**验收边界：** 以上属于客户端连通性与 KV/TTL/SCAN 冒烟测试，不能替代 Sentinel 故障切换、TLS 生产证书链、业务会话滚动升级、Bloom 跨实例、多级缓存订阅断线或 Stream/延迟队列崩溃恢复的验收。相关未完成事项继续跟踪 [Epic #18](https://github.com/Ryan-Guizhou/peach-cloud/issues/18)。

## 真实多实例失效测试（Issue #55）

`MultiCacheTwoClientServerIntegrationTest` 在 Redis 7.2 服务容器中使用两个独立 Jedis 连接工厂、两个 `MultiCacheManager` 和订阅容器，验证节点 A 更新 Redis L2 后，节点 B 的 Caffeine L1 经 Pub/Sub 失效并重新读到最新值。广播 JSON 与历史 Java 序列化约定一致：只传缓存名和发送者，不传业务 key；因此**远端按整个 cacheName 失效**。本地 key 与 Redis L2 key/value 格式保持不变。该机制是尽力通知，不能保证离线节点或断线重连期间的强一致；应设置有界 L1 TTL，禁止仅凭缓存作实时授权。

## 部署、验收与回滚

分步迁移、Nacos/Redis key/codec/Sa-Token session 兼容检查、实际 CI 测试证据、故障边界与回滚门禁见 [Redis / Redisson 迁移验收手册](../../docs/redis-redisson/acceptance-and-rollout.md)。真实连通性冒烟测试不代表生产 Redis 故障切换验收。

## Sa-Token 管理端检索安全性（Issue #57）

Sa-Token 的 `PeachSaTokenDao#searchData` 已由阻塞式 Redis `KEYS` 迁移至 `SCAN`，Cluster 遍历各主节点。默认最多累计 50000 个唯一匹配键，可通过 `peach.satoken.dao.search-max-matches` 调整；超过上限抛错，不静默截断。该操作不适合在线高频业务。详见 [Sa-Token 组件说明](../peach-satoken/README.md#redis-session-检索与有界-scanissue-57)。

## 旧/新客户端 JSON 兼容验收（Issue #65）

历史 `RedisConfig` 的 Jackson 序列化设置现在以不可依赖旧 jar 的测试夹具形式保留于 `peach-redis-quickstart`。真实 Redis 7.2 检验 Map 的 value/hash-value 字节一致、双向读写与 TTL；历史 PR [#66](https://github.com/Ryan-Guizhou/peach-cloud/pull/66) 曾验证仓库内旧/新实现。**这些都不包含真实已发布旧版 SaSession 二进制数据，也不保证生产滚动兼容。**

## 订阅断线补偿（Issue #67）

MultiCache 在 Redis Pub/Sub 的首次订阅及重新订阅确认时，**仅清除当前 JVM 已创建缓存的 Caffeine L1**，下次读取从 Redis L2 重新获取，弥补离线期间没有收到的广播（Redis Pub/Sub 不支持重放）。真实 Redis 7.2 测试使用两套独立 Jedis 连接，主动停止订阅、更新 L2，确认断线时旧值仍可能存在，再恢复订阅验证本地 L1 被清空并读取新值。此机制**不提供掉线期间的强一致性**；业务仍需合理设置 L1 TTL，鉴权与支付等关键决策必须以权威状态为准。

## 长生命周期 Sentinel 客户端故障恢复（Issue #69）

专项 CI 使用原有 `JedisConnectionFactory` / `RedissonClient` 实例完成真实读写，**实际停止隔离 Sentinel 主节点**，选主后不重建客户端，在有界期限内重试并验证 SET/GET/TTL；随后执行原有的三 Sentinel 共识及新客户端回归测试。仅执行本仓库 Redis 7.2 容器，不操作生产。通过这项测试可以确认对应客户端实例的基础写读恢复，但无法证明执行中业务事务、Stream / DelayQueue 消息幂等、真实历史 Session、Redis Cluster 故障等场景。

## Redis L2 剩余 TTL 保护（Issue #71）

MultiCache 在写入或从 Redis L2 回填 Caffeine L1 时，会读取该键的真实 Redis PTTL，并在本地维护有界的截止时间元数据。**纯 L1 命中不需要访问 Redis**；如果 Redis 剩余寿命已结束，或者本地截止时间元数据由于容量限制丢失，则清除 L1、重新查询 L2。PTTL 为 -1 的键仍按原有 Caffeine 过期策略执行，PTTL 不可确认时不缓存本地。特别处理了“靠近 Redis 过期时才回填”的情况；数据结构及 Redis key/codec 不变。Redis 外部改写或 Pub/Sub 掉线期间仍不提供强一致，使用本地 L1 作为安全决策依据是不可靠的。

## Redis Cluster 主节点故障演练（Issue #73）

在独立 Redis 7.2 CI 的三主三副本 Cluster 中，健康拓扑验收后实际停止 7001 主节点，确认它的原副本晋升、存活主节点数量恢复为 3、`cluster_state:ok`，再进行真实重定向 SET/GET/TTL 和四套 Jedis、Redisson、Redis Tool、Sa-Token 新客户端集成测试。该测试验证故障后的集群恢复与**新连接**，不能替代长连接原位重连、消息处理中的故障窗口、跨 AZ 容灾或高并发压测。见 [Issue #73](https://github.com/Ryan-Guizhou/peach-cloud/issues/73)，以专项 CI 结果判定是否通过。

## 真实 Redis ACL 正反例验收（Issue #75）

Redis 7.2 CI 增加实际启用 ACL 的独立单节点环境：禁用 default 用户、只开放随机临时密码的专用账户；独立 Jedis、Redisson、Redis Tool、Sa-Token 实际读写和 TTL 必须成功，匿名访问及错误密码必须被拒绝。测试密码仅在 GitHub Actions 临时作业生成，不进入源码或 Nacos。这验证的是**单节点数据 ACL 认证**，不代表 Sentinel 自身 ACL 的跨节点轮换、生产秘钥管理、TLS 双向认证或最小权限审计已经完成。

## 边界

- key 必须有稳定命名空间和必要隔离维度，敏感值不直接进入 key/log。
- 缓存必须明确 TTL、失效、回源和一致性策略。
- Stream 消费必须业务幂等，并明确 ACK、重试、积压和失败治理。
- Pub/Sub、Stream 和缓存都不能被文档包装成强一致或 Exactly-Once。

## 六个业务启动模块迁移（Issue #45）

Fileservice、Generator、Message、Setting、Monitor 和 Scheduled 的启动模块改为显式引用 `peach-redis-starter` 与 `peach-redisson-starter`，并删除直接声明的旧 `peach-redis-common`、Jedis、Redisson 和 Spring Data Redis 依赖。该批迁移保留两个客户端的可用性与 `peach.redis.*` 配置协议，不变更 key、codec 和已有会话数据。Scheduled 现有 `peach-redission-distributedlock-starter` 仍保留。网关继续使用独立响应式 Lettuce，不经这些 Starter。

**验收边界：** 本次 POM 依赖迁移不代表 Sentinel / Cluster / TLS、老会话滚动兼容或双节点故障注入已通过。旧 Common 模块暂留在 reactor 内供未迁移调用方使用；完成全仓引用与生产迁移验证后再考虑删除。
