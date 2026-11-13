# 生产运维与迁移手册

当前实现提供 at-least-once 投递；生产批准仍需[审计验收矩阵](production-readiness-audit-2026-11-13.md)和[整改状态](production-remediation-status.md)中未完成的外部验证。不得把本地测试成功或 `production=true` 作为发布批准。

## 上线与数据库迁移

1. 固定待发布应用、BOM、数据库/Broker/JDK/镜像版本，保存依赖树、SBOM、SCA 结果、配置摘要（不含 secret）与回滚包。确认两实例共享数据库的恢复、ACL/TLS、容量和业务幂等演练报告。
2. 保存当前 Topic、Group、订阅表达式、位点、待发/FAILED 数量、最老待发年龄、活跃租约、历史密钥 ID。备份数据库并实际验证恢复；不能通过清空幂等记录完成升级。
3. 新数据库使用 [MQ_CONSUME_RECORD.sql](../../../sql/MQ_CONSUME_RECORD.sql) 与 [MQ_OUTBOX_EVENT.sql](../../../sql/MQ_OUTBOX_EVENT.sql)。已有数据库由 DBA 对照实际表结构依次核对 [owner token 迁移](../../../sql/migrations/20261112_rocket_idempotent_owner_token.sql)、[发送选项迁移](../../../sql/migrations/20261113_rocket_outbox_send_options.sql)、[生产加固迁移](../../../sql/migrations/20261113_rocket_production_hardening.sql)。这些是版本化迁移，不得对新库或已迁移库盲目重跑。DDL 不由 Starter 自动执行。
4. 变更前停止旧 Dispatcher 和旧无 token 写入者，等待已认领工作结束或到达租约恢复条件；不能让旧写入绕过新 owner fencing。安排 DDL 锁、索引、大小写/排序规则及数据约束检查。DDL 只在备份和预演后由独立迁移账号运行。
5. 数据库会话/JDBC 与运维工具统一时区（推荐 UTC）；记录实际隔离级别、时区、精度和排序规则。租约依据数据库时间；监控节点时钟偏差。消费 processing lease、成功去重 TTL 与 Outbox retention 分开配置，不能把 processing lease 当去重保留期。
6. 配置 `production=true` 与[生产配置基线](production-configuration.md)，确认同一业务 DataSource、生产 Store 能力、受管执行器容量与最小权限。先验收实例再逐步放量，记录配置与迁移批次。

## 协议、密钥与位点滚动升级

新信封/事件去重语义应先部署兼容读端再切新写端；明确哪些旧版本能读哪些信封，不能让不理解 AAD 的旧消费者读取新消息。`accept-legacy-encryption=true`、`algorithm=AES_GCM` 和 `legacy-idempotent-key=true` 仅可在受控非严格迁移阶段使用；生产严格模式禁止这些设置。不存在自动旧幂等键双读保证，必须由迁移设计显式处理旧记录、重放窗口和位点，必要时暂停流量。不能通过关闭安全或删除去重表绕过迁移问题。

轮换时先将新旧密钥同时下发读端，再切活动 `key-id`。旧密钥保留至 Broker、Outbox、DLQ、归档和备份的允许重放窗口全部结束；之后才撤销，并验证未知 ID 失败。禁止日志打印密钥、ACL hook、完整 Header/载荷、签名 URL。新写入已不可被旧应用识别后，回滚必须保留兼容读端；先停新写入再按协议矩阵回退。

`auto-prefix-consumer-group=false` 默认保护现有 Group，严格生产模式要求 true，因此从默认开发配置进入生产必须显式迁移。修改 namespace、Topic 命名、Group 前缀、订阅或 `consume-from-where` 前先规划位点和去重域；FIRST_OFFSET/LAST_OFFSET 仅影响全新 Group，不能用它重置旧 Group。相同 Group 必须使用一致订阅。变组可能重放旧消息或跳过历史消息，必须有业务批准和可验证的位点操作记录。

## 业务事务与幂等责任

生产普通 Outbox `publish` 要求当前事务确实绑定 Store 的同一个 DataSource；只有 `@Transactional` 字样但跨库、错误事务管理器或代理未生效仍不满足契约。通过同库业务行+Outbox 行提交/回滚与进程崩溃验证原子性。`publishStandalone` 是显式独立发布入口，不承诺与业务状态原子提交，不应冒充业务事务可靠消息。发送和数据库确认之间仍没有分布式原子事务。

Store owner token 防止过期持有者覆盖 Store 状态，租约续期减少正常长任务误接管；它不隔离任意外部副作用。消费续租失败不得继续确认处理成功，但不能回滚已经发生的外部副作用；续租调度与关闭也须演练。Handler 必须在业务事务中用事件唯一键/业务状态机约束重复操作，或要求下游接受稳定幂等键。业务已提交、ACK 前崩溃、网络超时重发、租约到期新旧实例并行均必须不重复扣款/回调。不能将 `markSuccess` 与业务副作用视为天然同一事务。

## 故障处理与重放

| 现象 | 证据与判断 | 操作与验证 |
| --- | --- | --- |
| Outbox 积压增长 | PENDING/SENDING/FAILED 数量、最老年龄、发送/ACK 失败指标、DB 池和 Broker RTT | 先修 ACL/TLS、Broker、DB 或容量原因；恢复扫描后观察积压下降，避免无限扩容冲击下游 |
| SENDING 长时间未完成 | claim token、CLAIMED_AT、租约、进程存活、DB 时间 | 确认原 owner 是否仍活动；由 Store 的租约恢复领取，禁止无条件手改状态抢占 |
| Broker 成功但 ACK 失败 | 区分 send_failed 与 ack_failed，按稳定事件 ID 关联 | 恢复数据库后允许重投；旧 owner 不得覆盖新 owner，业务验证无重复副作用 |
| 单条损坏记录阻塞 | 隔离 FAILED、脱敏 LAST_ERROR、字段格式/发送选项 | 实现隔离坏行并继续正常行；备份审查原始记录，仅有授权者修复，修复后再重放 |
| 解密/协议失败 | 算法、keyId、路由、版本及脱敏错误分类 | 确认密钥分发/协议灰度或攻击；不把所有消息降级为明文接收 |
| 消费持续重试/DLQ | Broker 重投次数、DLQ 数量/最老年龄、业务错误分类 | 修复根因与业务幂等，保留原事件 ID；有限批次重放，观察业务结果后继续 |
| 失败记录服务不可用 | `MqFailureRecorder` 结果和消费重试 | 不允许静默 SKIP/ACK；恢复可靠失败存储，核对补偿工单 |

`MqOutboxReplayService` 是程序化 SPI，当前模块没有管理 HTTP endpoint。通过受控管理作业调用 `replay(messageId)` 并核对返回值及后续投递结果；不返回成功就不宣称恢复。FAILED 重放需记录操作者、授权范围、原因、事件 ID、前后状态、工单及业务核对结果，限制批量和速率，防止重复提交。若业务自行增加 HTTP/控制台入口，必须另行实施身份认证、Topic/租户授权和审计；不能假设 SPI 已具备这些能力。

JDBC `cleanupTerminal(limit)` 提供有界清理，单批最多 500；不是默认自动归档服务。Outbox 会清理过 retention 的无认领 SENT/FAILED 记录，因此先归档并确认 FAILED 已有处置结论和重放需求。消费成功记录清理必须遵守完整去重窗口；不得删除未完成记录以“解锁”。清理作业账号、调度、备份恢复、归档保留和审批由部署方管理。归档重放也须保留事件 ID 与业务去重记录。

## 告警与停机

接入 `peach-observability-starter` 和 `MeterRegistry` 可提供 `peach.rocket.publish.*`、`consume.*`、`outbox.total`、`consumer.backpressure.total`。未接入时可能是 NOOP。Broker backlog/DLQ、数据库 FAILED/最老年龄/过期认领、UNKNOWN 回查、解密失败和 ACK 失败需额外采集，不能假称内置全套 Gauge。使用固定低基数标签，不用事件 ID、业务 Key、租户 ID 作为指标标签。每类告警需有值班负责人、阈值、SLO 与演练触达证据。

停机顺序：先撤流量并拒绝新业务发布，停止 Outbox 新扫描/新认领；等待已提交发送与消费任务在预算内排空；再关闭事务 Producer、普通 SDK、Consumer 及执行器/数据源。平台 termination grace period 须覆盖所有步骤。确认当前各发送路径真实关闭行为，通过 SIGTERM、强杀、异步回调未返回、事务本地执行中、Outbox 排队中断验证；不能因 Consumer 有 drain 就声称全部发送路径已验收。超时或强杀后的工作依靠租约、重投与业务幂等恢复，线程取消不会撤销已发生的外部副作用。

## 验证入口与发布证据

从仓库根目录用 Java 21 执行受影响模块测试及 `node scripts/verify-changes.mjs`；发布还必须执行当前仓库提供的真实数据库/Broker/安全验收入口，并检查执行结果与 skipped 数量。测试入口见 [JDBC tests](../peach-rocket-jdbc/src/test/java/com/peach/rocket/jdbc/) 与 [quickstart tests](../peach-rocket-quickstart/src/test/java/com/peach/rocket/quickstart/)，实际参数以对应测试和发布脚本为准。依赖版本以[当前 BOM](../../../peach-dependencies/pom.xml)和最终应用依赖树为准。

本次环境 Docker engine 不可用，真实 MySQL、Broker ACL/TLS、多实例故障和最终产物 SCA 仍需外部报告。没有真实环境证据时保持待验收，扫描失败/服务不可用不等于无漏洞。发布记录必须包括版本、执行时间、环境、场景、结果、未通过项和批准人；[历史审计](production-readiness-audit-2026-11-13.md)保持快照，不回写成整改完成。
