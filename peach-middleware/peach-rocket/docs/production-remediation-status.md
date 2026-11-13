# 生产整改状态

基于 2026-11-13 审计快照与当前整改源码。原[审计报告](production-readiness-audit-2026-11-13.md)保留历史证据；本文记录仓库整改和未完成验收，**不代表生产验收通过**。范围和未投产决策见[实施计划](production-hardening-plan.md)，配置与操作分别见[Reference](production-configuration.md)和[运维手册](production-operations.md)。

“已实现”表示当前源码存在对应保护；需结合本轮实际测试报告复核，不自动等同真实环境关闭。所有行仍受最终应用安全扫描、真实 MySQL/Broker 和业务幂等验收约束。

| 发现 | 当前整改/限制 | 剩余验收或责任 |
| --- | --- | --- |
| RKT-01 Key 表达式 | 运行时 Key 为字面值；注解限只读属性读取 | 确认业务使用字面 Key 或只读属性读取，无旧表达式执行入口 |
| RKT-02 接收加密策略 | 接收端执行本地加密要求，拒绝明文降级 | 真实链路恶意输入/灰度验证 |
| RKT-03 认证信封 | AES_GCM_AAD 认证信封元数据并核对 Broker 路由；无历史非认证协议入口 | 篡改与错误算法端到端验证 |
| RKT-04 ACL/TLS | 统一客户端安全适配；Peach canonical 凭据冲突失败；production 要求 ACL/TLS | 四类客户端错误身份/证书/越权及轮换实测 |
| RKT-05 供应链 | 依赖收敛以当前 BOM 为准，提供扫描门禁方向 | 最终应用/镜像/JDK/SBOM 与有效全量 SCA 报告待完成；不能关闭 |
| RKT-06 日志遥测 | 脱敏错误信息与低基数指标，避免输出完整消息/Key | 最终应用日志及 tracing 泄密扫描，运维接入审查 |
| RKT-07 事务 Key | 使用 SDK 合法业务 Key 写入路径，避免保留属性冲突 | 真实事务发送/回查与 Broker 权限验证 |
| RKT-08 业务 fencing | Store owner token 与续租保护状态更新；不承诺隔离任意业务副作用 | 业务唯一约束/下游幂等与崩溃重投证明，业务负责人负责 |
| RKT-09 稳定事件 ID | 默认由信封 messageId 去重，无已删除的历史模式开关 | 所有发送路径保持事件 ID 稳定；业务重放时不重写 ID |
| RKT-10 Outbox 事务 | production 普通 publish 强制同 DataSource 活跃事务；standalone 明确独立语义 | 实际数据库提交/回滚、错误事务管理器与 kill 验证 |
| RKT-11 租约 | 独立 processing/outbox lease、owner-scoped 续租/ACK、超时预算校验 | 长任务、排队、失租接管与多实例故障演练 |
| RKT-12 顺序 | production 拒绝顺序 Outbox；不宣称已实现跨实例严格全局顺序 | 严格顺序 Unsupported；普通顺序路由的业务约束另行验证 |
| RKT-13 Store 能力 | production 对持久化、fencing/续租能力失败关闭 | 自定义 Store 必须真实数据库并发/恢复验收，接口声明不是证明 |
| RKT-14 广播 | production 拒绝广播消费者 | 生产广播 Unsupported；不能共享 Store 冒充每实例消费保证 |
| RKT-15 Group/位点 | 首次消费位置可配，Group/订阅一致性约束；新项目默认启用 Group 环境前缀 | 首次部署确认组名、订阅和初始消费位点 |
| RKT-16 坏行 | 损坏存储记录隔离失败并继续正常行 | 真实坏行、告警、授权修复与有限重放演练 |
| RKT-17 资源预算 | 编码并发、发送/消费准入、字节/Header/JSON 深度字符串数字限制 | 实际负载、DB 池耗尽和执行器拒绝压力验收 |
| RKT-18 信封一致 | 发送路径保留稳定事件身份、版本和生产应用元数据 | 最终业务自定义 Codec/SPI 必须回归 |
| RKT-19 数据库约束 | 分离租约与 TTL/retention；有界终态清理与统一初始化 DDL | **必须验证实际表引擎支持事务与行级锁**，并实测 MySQL 时间、索引、默认排序规则比较语义、回滚、锁及归档恢复；未通过前不得投产 |
| RKT-20 超时 | 未指定继承默认，显式单次覆盖；租约覆盖发送预算 | 实际 SDK/Broker 重试耗时与不确定发送结果演练 |
| RKT-21 失败重放 | 保留可靠记录后 SKIP、程序化 Replay SPI、ACK 失败分类及运维流程 | 默认记录器/指标不能代替运维系统；DLQ、告警触达、授权重放待接入 |
| RKT-22 关闭排空 | Consumer 有界排空与停止准入；记录发送路径关闭契约 | 所有发送/事务/Outbox 路径 SIGTERM/强杀验收未完成，不宣称全部关闭 |
| RKT-23 发布门禁 | 提供本地回归与真实环境入口，要求失败/跳过可辨识 | Docker engine 不可用；真实 MySQL/Broker、多实例、安全和容量报告待完成，OPEN |
| RKT-24 演示文档 | 中英 README 对齐当前用户 Runner 快照，未覆盖其改动 | 当前 Runner 仅普通发送，未等待消费；不能据日志宣称消费已完成 |

## 证据索引与状态更新

- 配置/启动保护：[自动配置源码](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/autoconfigure/)、[客户端适配](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/support/MqClientSecurity.java)。
- 协议/路由/日志：[codec](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/codec/)、[security](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/security/)、[route](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/route/)、[consumer](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/consumer/)。
- 认领与事务：[Outbox](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/outbox/)、[幂等](../peach-rocket-autoconfigure/src/main/java/com/peach/rocket/idempotent/)、[JDBC](../peach-rocket-jdbc/src/main/java/com/peach/rocket/jdbc/)。
- 回归证据：[核心测试](../peach-rocket-autoconfigure/src/test/java/com/peach/rocket/)、[JDBC 测试](../peach-rocket-jdbc/src/test/java/com/peach/rocket/jdbc/)、[quickstart 测试](../peach-rocket-quickstart/src/test/java/com/peach/rocket/quickstart/)。测试总数以本轮最终 Maven/Surefire 结果为准；历史审计的 54 通过/1 跳过仅属于旧快照，不是整改后结果。

生产批准前必须逐项附上版本化测试和外部验收证据，由框架、业务、DBA/平台、QA、安全负责人确认。仍待验收的环境项、Unsupported 的模式与业务职责不得统一标记 CLOSED。
