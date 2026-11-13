# Peach Rocket 生产整改实施计划

> 执行基于用户“按整改文档一次性整改”的授权。审计快照保持历史记录，最终状态单独记录。不得将本地测试替代真实集群/安全验收。

## 实施契约

- Goal：关闭可在仓库内修复的安全、协议、认领、配置与测试缺口，提供可执行生产配置和真实环境验收入口。
- Scope：Rocket 四模块、相关 SQL、必要 Scheduler/Observability 适配、依赖 BOM、安全检查与文档。
- Non-goals：不承诺 Exactly-Once；不自动修改外部数据库/部署生产；业务外部副作用仍需业务唯一约束/下游幂等。
- Compatibility：本模块尚未投产，不保留旧构造器、无 owner token 的状态更新、旧密文或旧幂等键；一次性收敛调用者、配置与测试。生产模式拒绝内存/无 fencing Store、广播和未证明严格顺序的 Outbox。
- Constraints：Java 21、UTF-8 无 BOM、中文 Javadoc/English logs；不修改用户已有 Runner 变更，不提交密钥，不自动提交或推送。
- Database：使用单一最终 DDL 初始化新库；不保留 Rocket 历史迁移脚本。字符集、排序规则及存储引擎采用数据库默认配置，在生产验收时核对唯一索引与大小写语义。
- Verification：缺陷回归、模块 Maven 测试、JDBC 集成测试入口、Broker 安全验收入口、依赖扫描和仓库门禁；环境不可用的项目保持待验收。
- Risks：新旧加密/去重语义、业务幂等、版本升级、MySQL 锁与时区、真实 TLS/ACL。

## 任务

1. 配置与生产门禁：新增 production、client、protocol 和可靠性属性，严格边界校验，metadata 一致。
2. 协议与安全：受限属性表达式、AAD 信封、禁止明文降级、JSON/报文预算、稳定事件 ID、脱敏日志。
3. JDBC/Outbox：durable/fencing 能力、token-only 状态更新、续租、DB 时间、TTL、坏行隔离、显式事务发布、统一信封/预算、严格顺序拒绝。
4. SDK 与消费：统一 ACL/TLS/namespace/timeout，修复事务 Key、回查路由与组，订阅一致性、Broker 路由核验、广播限制、关闭排空。
5. 依赖与验收：安全版本收敛、最终依赖清单/SBOM 与漏洞扫描入口、真实 MySQL/Broker 验收配置；不静默跳过发布验收。
6. 文档与复核：配置属性/默认值/单位/适用范围/组合约束、生产示例、迁移/运维与每项审计关闭证据；独立复核和最终验证。

## 接口交接

配置由主执行者维护；其他任务不编辑 PeachRocketProperties、metadata、README 或 BOM。

- `isProduction()` 默认 false；开启后要求 durable/fenced 幂等与 Outbox、安全 client、关闭自动 Topic 和广播，Outbox 顺序发送拒绝。
- `getClient()`：accessKey/secretKey 字符串默认空，tlsEnabled 默认 false；凭据成对，生产强制 TLS。
- 加密协议只支持 `AES_GCM_AAD`，不接受非认证信封或旧 `AES_GCM`；密钥仅经 keyId 映射读取。
- 幂等键只使用消息信封的稳定事件 ID，拒绝缺失 event ID 的消息；无旧键回退。
- `getJdbc().getProcessingLease()` 默认 5m；`getJdbc().getOutboxLease()` 默认 5m；`getJdbc().getRetention()` 默认 7d；DB 操作使用数据库时间。
- `getOutbox().isRequireTransaction()` 默认 false；生产强制 true；无业务事务的结果事件可显式 `publishStandalone`。
- Store 增加 `isDurable()`、Outbox `supportsOwnerFencing()` 默认 false；JDBC 实现返回 true。`renew(claim)` 默认 false，供 token 有效性/续租检查。

## 进度

仓库内可修复项已经落地，并通过受影响模块测试与 `node scripts/verify-changes.mjs` 复核。下列勾选只表示源码、初始化 DDL 和本地回归入口存在，不表示真实集群验收通过。

- [x] 配置与生产门禁
- [x] 协议与安全
- [x] JDBC/Outbox
- [x] SDK 与消费
- [x] 依赖与验收入口（BOM 收敛和可识别的真实环境测试入口；最终 SBOM/SCA 与集群报告仍待外部执行）
- [x] 文档与源码复核（环境不可用项保持待验收，见整改状态）

真实 MySQL、Broker ACL/TLS、多实例故障和最终产物扫描仍按[整改状态](production-remediation-status.md)保持未关闭。不得把 skipped 测试或 `production=true` 写成发布批准。

Ruling：当前工作区包含用户修改且 main 受保护，创建本地整改分支保留现有修改；不创建/提交额外无关副本。
Ruling：真实环境不可用时交付明确失败的验收入口和未关闭记录，不以跳过或 Mock 证明生产可用。

## 本轮未投产前的安全验收结论（Issue #200）

**结论：仓库内高风险兼容入口已经收敛，但不代表生产安全隐患全部消除。** 当前版本尚未投入生产，直接采用最终表结构，已删除旧迁移脚本、无 owner token 状态更新 API、历史非认证 AES-GCM、旧幂等键和单密钥回退。下表是必须完成的剩余事项，严格区分实现完成与外部验收。

| 优先级 | 风险/审计项 | 当前状态 | 关闭条件与建议处理 |
| --- | --- | --- | --- |
| P0 | RKT-04 统一 ACL/TLS 身份与证书认证 | 代码适配已实现，真实集群**未验收** | 用实际 NameServer/Broker 对普通 Producer、动态 Consumer、事务 Producer、Topic Admin 分别做正确凭据/错误凭据、错误证书、越权拒绝测试；安全负责人复核 |
| P0 | RKT-08/10/11 业务幂等、事务一致性和租约丢失 | token-scoped Store 已实现，业务副作用**不能单靠框架 fencing 防止** | MySQL 真实环境两实例并行认领、进程 kill/恢复、事务回滚、长任务失租；业务表用唯一键和状态机/CAS 兜底，第三方调用带稳定幂等 ID |
| P0 | RKT-19 数据库默认引擎/排序规则 | DDL 无显式 ENGINE/CHARSET/COLLATE，**依赖环境默认值** | 部署前验证实际表使用事务型引擎，具备行锁/回滚；校验 ID、业务 Key、Group 唯一索引在默认 Collation 下不会误合并大小写不同的标识 |
| P0 | RKT-05 供应链与依赖漏洞 | BOM 有调整，**未提供完整 SCA 放行报告** | 生成最终服务的 SBOM、依赖收敛树，扫描 JDK/镜像/shaded 网络依赖，严重漏洞修复或留有书面风险豁免 |
| P1 | RKT-02/03/06/17 协议攻击面与资源拒绝服务 | 限制、认证信封与脱敏策略代码已具备，**缺少恶意输入/负载证据** | 编解码篡改、恶意大体积/深嵌套、Header、错误 keyId/算法、敏感日志扫描、突发峰值/背压和内存占用测试 |
| P1 | RKT-16/20/21 坏行隔离、超时、重试和死信 | 相关机制已实现，**运维闭环未接入** | 构造坏行和不可达 Broker、观察失败记录与重试预算；为 DLQ/FAILED 建立告警、授权重放入口、审计和人工补偿流程 |
| P1 | RKT-22/23 关闭排空与发布门禁 | 部分生命周期测试具备，**真实故障/多实例验收未完成** | SIGTERM/强杀、多 Broker 异常、消息 ACK 不确定性、恢复与回归；受影响 Maven 模块和 CI 全部门禁通过后方可提交合并 |
| 限制 | RKT-12/14 跨实例严格顺序 Outbox 与广播消费者 | 当前生产严格模式明确拒绝 | 不承诺已支持；若业务需要，先提出独立设计、吞吐与正确性验收，不通过关闭保护开关冒充支持 |

**操作原则：** 未有真实环境证据前不得将上述状态从「待验收」改为「已排除」；本次只处理 PR #199 的相关代码与文档，不以扩展无关 CI 来代替安全验收。
