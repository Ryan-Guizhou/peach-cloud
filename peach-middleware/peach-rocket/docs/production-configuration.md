# 生产配置 Reference

本文对应当前源码，不替代生产验收。默认配置用于开发；`production=true` 是启动与调用路径的严格校验，不能证明外部 Broker、数据库或证书已正确部署。配置事实源为 [PeachRocketProperties](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/autoconfigure/PeachRocketProperties.java)、[客户端适配](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/support/MqClientSecurity.java) 和 [JDBC 实现](../peach-rocket-jdbc/src/main/java/com/peach/rocket/jdbc/)。

## 配置基线

业务依赖 `peach-rocket-starter`；持久化还须显式依赖 `peach-rocket-jdbc`，提供与业务事务绑定的同一 `DataSource`，按[运维手册](production-operations.md)使用最终 DDL 初始化。以下环境变量均须由部署平台安全注入，无密钥默认值：

```yaml
rocketmq:
  name-server: ${MQ_NAME_SERVER}
  producer:
    group: order-service-producer
peach:
  rocket:
    production: true
    namespace: prod
    app-name: order-service
    naming:
      auto-prefix-consumer-group: true
    transaction:
      producer-group: order-service-transaction-producer
    client:
      access-key: ${MQ_ACCESS_KEY}
      secret-key: ${MQ_SECRET_KEY}
      tls-enabled: true
    jdbc:
      enabled: true
      processing-lease: 5m
      outbox-lease: 5m
      retention: 7d
    consumer:
      require-durable-idempotency: true
      idempotent-expire: 7d
    security:
      enabled: true
      encrypt-payload: true
      algorithm: AES_GCM_AAD
      base64-key: true
      key-id: current
      keys:
        current: ${MQ_KEY_CURRENT_B64}
    outbox:
      enabled: true
      require-durable: true
      require-transaction: true
    topic:
      auto-create: false
```

`idempotent-expire: 7d` 是示例业务重放窗口，默认值仍为 `24h`。按实际 Broker 保留、DLQ、Outbox、人工重放窗口选择足够长的幂等保留期，并承担容量成本。不能仅调整 Outbox retention 就获得同等去重窗口。

## 全量 Peach 配置

表中键均以 `peach.rocket.` 为前缀。Duration 支持 `200ms`、`3s`、`5m`、`24h`、`7d` 等 Spring Duration 写法；字节值为整数，字符限制不是字节限制。每行最后一列同时说明约束与生产含义。

| 键 | 默认值 | 类型/单位 | 含义、约束与生产影响 |
| --- | --- | --- | --- |
| enabled | true | boolean | 自动配置总开关；关闭后不提供 Peach 运行保障 |
| production | false | boolean | 严格模式；生产显式 true，拒绝开发降级和不支持模式 |
| namespace | default | String | Peach 环境命名前缀；1–64 位 ASCII 字母/数字/下划线/点/横线，生产不可为 default |
| app-name | application | String | 信封生产应用标识；同 namespace 格式，生产不可为 application |
| client.access-key | 空串 | String | ACL 身份；与 secret-key 成对，生产必填；安全注入 |
| client.secret-key | 空串 | String | ACL 密钥；禁止日志/仓库保存；生产必填 |
| client.tls-enabled | false | boolean | SDK TLS 开关；生产必须 true，仍需部署证书与信任配置 |
| protocol.max-depth | 64 | int/层 | JSON 嵌套深度，1–256；本地解析预算，不信任消息自报值 |
| protocol.max-string-length | 1048576 | int/字符 | 单个 JSON 字符串长度，正数；密文 Base64 也占字符串预算 |
| protocol.max-number-length | 128 | int/字符 | JSON 数字长度，正数；避免超长数字解析资源耗尽 |
| producer.default-timeout | 3s | Duration | 默认发送超时，1ms–60s；单次选项显式值优先 |
| producer.max-encoding-inflight | 64 | int/任务 | 并发编码/加密额度，正数；与发送额度分别限制 |
| producer.max-payload-bytes | 4194304 | int/字节 | 编码后信封上限，正数；必须同时满足 Broker 限制 |
| producer.max-header-count | 64 | int/项 | Header 项数上限，包含业务 Key；正数 |
| producer.max-header-bytes | 65536 | int/字节 | Header Key/Value UTF-8 总量，正数；禁止装入 secret |
| producer.max-inflight | 1024 | int/请求 | 单 Producer 在途上限，正数；非集群限流 |
| producer.inflight-acquire-timeout | 200ms | Duration | 发送准入等待，0–30s；0 为立即拒绝 |
| consumer.enable-idempotent | true | boolean | 消费 Store 去重；生产必须 true，仍需业务幂等 |
| consumer.consume-from-where | LAST_OFFSET | String/枚举 | FIRST_OFFSET 或 LAST_OFFSET；仅影响新 Group 初始位点 |
| consumer.idempotent-expire | 24h | Duration | 成功去重记录 TTL，正数；覆盖完整重放窗口 |
| consumer.dynamic-register | true | boolean | 扫描并注册注解消费者；关闭后自行负责消费接入 |
| consumer.consume-thread-min | 1 | int/线程 | 单 SDK Consumer 最少工作线程，正数 |
| consumer.consume-thread-max | 20 | int/线程 | 单 SDK Consumer 最多工作线程，不小于 min |
| consumer.in-memory-max-entries | 32768 | int/条 | 开发内存幂等容量，正数；生产不可依赖其持久性 |
| consumer.require-durable-idempotency | false | boolean | 强制持久化与 owner fencing 能力；production 自动要求 |
| consumer.max-dynamic-consumers | 64 | int/实例 | 单 JVM 动态 Consumer 上限，正数；超限启动失败 |
| consumer.max-inflight | 128 | int/任务 | 动态消费者共享 Handler 并发额度，正数 |
| consumer.inflight-acquire-timeout | 200ms | Duration | 消费准入等待，0–30s；超限返回重试而非 ACK |
| consumer.shutdown-drain-timeout | 30s | Duration | 有界排空，1ms–5m；平台宽限期还须覆盖其他关闭步骤 |
| naming.topic-prefix | biz | String | Topic 前缀，非空；变更会改变路由 |
| naming.group-prefix | cg | String | Group 前缀，非空；仅相应开关启用时应用 |
| naming.topic-separator | - | String | 命名分隔符，非空；变更需迁移 Topic/位点 |
| naming.auto-prefix-env | true | boolean | Topic 增加环境前缀；按最终解析名称预建 Topic |
| naming.auto-prefix-consumer-group | true | boolean | Group 增加环境与组前缀；新项目默认开启，首次部署前固定组名 |
| security.enabled | false | boolean | 安全 Codec 开关；生产必须 true |
| security.encrypt-payload | false | boolean | 全业务载荷加密策略，接收端也拒绝所要求消息的明文降级；生产 true |
| security.algorithm | AES_GCM_AAD | String | 只支持认证信封算法 AES_GCM_AAD，其他值启动失败 |
| security.key-id | default | String | 活动加密密钥 ID；信封携带 ID，未知 ID 拒绝 |
| security.keys | 空 Map | Map<String,String>/秘密 | ID 到 AES 密钥映射；解码后应为合法 AES 长度 16/24/32 字节 |
| security.base64-key | false | boolean | true 时配置密钥为 Base64；否则按原始文本字节解释 |
| security.encrypt-topics | 空 List | List<String> | 附加按 Topic 加密策略；生产全量 encrypt-payload 不依赖此列表 |
| transaction.enabled | true | boolean | 事务消息自动配置开关；不同于业务事务 Outbox |
| transaction.producer-group | peach-rocket-transaction-producer | String | 独立事务 Producer Group；生产启用事务消息时须显式设置非默认组名 |
| topic.auto-create | false | boolean | 应用启动尝试建 Topic；生产必须 false |
| topic.read-queue-nums | 4 | int/队列 | 自动建 Topic 的读队列数，正数；生产由平台治理 |
| topic.write-queue-nums | 4 | int/队列 | 自动建 Topic 的写队列数，正数；调整影响顺序路由 |
| topic.include-consumer-topics | true | boolean | Topic 治理发现注解消费者声明；非 Broker 订阅权限授予 |
| topic.include-transaction-topics | true | boolean | Topic 治理发现事务处理器声明 |
| topic.topics | 空 List | List<String> | 额外 Topic 声明；仍需平台预建与 ACL |
| jdbc.enabled | false | boolean | 可选 JDBC 自动配置；须显式引入模块、数据源、DDL |
| jdbc.processing-lease | 5m | Duration | 消费处理租约，1ms–1h；与成功记录 TTL 分开 |
| jdbc.outbox-lease | 5m | Duration | Outbox 租约，1ms–1h 且大于默认发送超时两倍；逐条超时也受租约约束 |
| jdbc.retention | 7d | Duration | 无认领 SENT/FAILED 终态清理年龄，至少 1h；清理前先归档/审批 FAILED |
| outbox.enabled | false | boolean | 开启 Outbox 与 Dispatcher；生产开启时要求持久化/fencing/续租能力 |
| outbox.require-transaction | false | boolean | 普通 publish 要求绑定同一数据源业务事务；production 强制 |
| outbox.batch-size | 50 | int/条 | 每轮认领上限，启用时 1–500；结合租约和吞吐规划 |
| outbox.scan-interval-ms | 2000 | long/毫秒 | 扫描间隔，启用时正数；不是 Duration |
| outbox.require-durable | false | boolean | 强制持久化 Store；production 强制，不能静默回退内存 |
| outbox.in-memory-max-entries | 1024 | int/条 | 开发内存容量，启用时正数；耗尽拒绝而不删除待发记录 |
| outbox.in-memory-max-payload-bytes | 65536 | int/字节 | 开发内存每条载荷预算，启用时正数 |
| outbox.in-memory-retention | 24h | Duration | 开发内存终态惰性清理年龄，启用时正数 |
| outbox.in-memory-lease | 5m | Duration | 开发内存认领租约，启用时正数 |
| outbox.parallel-enabled | false | boolean | 普通 Outbox 可选并行；需要受管有界 ExecutorService |
| outbox.executor-bean-name | rocket-outboxVirtualExecutor | String | 并行执行器 Bean 名，启用并行时非空；自行配置容量 |

## 原生 RocketMQ 与部署安全

`rocketmq.name-server` 和 `rocketmq.producer.group` 属于原生 RocketMQ Spring 配置。Peach `namespace` 用于环境命名；原生 `rocketmq.producer.namespace` / `namespace-v2` 是 Broker SDK namespace，两者不同，不能相互代替。普通模板、独立动态 Consumer、事务 Producer、Admin 使用统一客户端适配。Peach ACL 为规范入口；若同时配置原生 producer ACL，必须一致，否则拒绝冲突。避免维护两套凭据。

生产启动 JVM 还须显式设置 `-Dtls.test.mode.enable=false -Dtls.client.authServer=true -Dtls.client.trustCertPath=<已挂载且存在的 CA 文件绝对路径>`。这是 JVM/SDK 参数，不属于 `peach.rocket.*`。TLS=true 与 CA 文件存在不证明握手成功；部署团队必须按照实际依赖与 Broker 版本完成 CA 加载、证书认证、需要时的 mTLS、主机名策略及 Broker TLS listener，并验证错误证书、错误身份和越权 Topic/Group 均失败。

业务身份仅授予所需 Topic 发送、Group 消费及必要重试权限；Topic 管理使用独立运维身份。数据库账号限制于目标表所需 CRUD，建表 DDL 使用独立账号。通过 Secret Manager、受控配置服务或挂载 secret 注入凭据与密钥；限制读取权限、审计访问、设置轮换流程。示例变量名不是明文配置许可。

## 协议、超时与安全边界

认证信封绑定路由与元数据，消费端核对实际 Topic/Tag；未知算法、非法版本/类型、篡改、缺少解密器与不满足接收策略的明文均应失败。嵌套深度、字符串、数字、编码字节、Header 与并发编码预算须联合配置：4 MiB 信封上限并不意味着允许 4 MiB 单字符串，Base64 密文还会膨胀。应用不得为远端类型启用不受控多态反序列化。

运行时 `MqSendOptions.key` 是字面值；`@MqEvent.key` 仅允许只读属性路径（如 `#orderId`），不允许任意方法、构造、类型、Bean 引用。业务查询 Key 不等于事件去重 ID；默认用信封稳定 `messageId`（event ID），重发同一事件必须保留该 ID，不能改用 Broker 重新分配的消息 ID。自定义 Header 拒绝 SDK 保留属性、id/timestamp/内部事务键及 authorization/password/secret/token/cookie 等敏感名称和控制字符。配置绑定拒绝未知属性，拼错键不能静默使用默认值。

单次 timeout 优先于 producer 默认值，未指定才继承默认值。SDK 自带重试、Broker 重投与 Outbox 重试属于不同层；Peach 没有可配置的统一“总重试次数”保证，不要把一层 timeout 当作端到端截止时间。网络超时可能已经投递成功，Store ACK 失败也可能导致重发；必须通过稳定事件 ID 和业务幂等应对不确定结果。事务回查必须查询持久化业务结果，UNKNOWN 不等于回滚。

严格模式拒绝广播消费者和顺序 Outbox；普通 `publishOrderly` 的同分片路由不证明多实例严格全局顺序。跨实例严格顺序为 Unsupported。自定义 Codec/Store/Publisher 必须重新验证这些安全与持久化契约，不能仅凭实现接口通过生产验收。部署步骤、恢复责任和外部验收见[运维手册](production-operations.md)及[整改状态](production-remediation-status.md)。
