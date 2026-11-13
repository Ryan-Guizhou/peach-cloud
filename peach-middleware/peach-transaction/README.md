# peach-transaction

[English](README.en-US.md) | 中文

Peach Cloud 的可扩展分布式事务接入 Starter。当前支持 **Seata 2.5.0 AT**；TCC、XA、Saga 仅作为后续候选模式，**目前不可用**。本家族不会重复实现 RocketMQ 事务消息或 Outbox。

## 组件与能力矩阵

| 模块 | 职责 |
| --- | --- |
| `peach-transaction-autoconfigure` | 公共 SPI、事务策略、属性、启动校验和配置元数据 |
| `peach-transaction-seata` | Seata AT SPI Provider；依赖 SCA 官方 Starter |
| `peach-transaction-starter` | 最小引入入口 |
| `peach-transaction-quickstart` | 不依赖外部服务的装配烟雾测试；不是双库回滚验收 |

| 能力 | 状态 |
| --- | --- |
| 官方 `@GlobalTransactional` + AT | 集成入口已接入；真实 TC/MySQL 联调待验证 |
| Java SPI `GlobalTransactionExecutor` / `GlobalTransactionOperations` | 已提供 |
| SPI Provider 自定义 Bean 覆盖 | 已提供 |
| Feign XID 传递 / 数据源自动代理 | 委托 SCA/Seata；需真实跨服务验证 |
| TCC / Saga / XA | 未实现 |
| RocketMQ Outbox / 消费幂等 | 使用已有 peach-rocket |
| 同服务 AT + Outbox 启动防护 | 默认禁止；不代表已有全局提交屏障 |

## 引入

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-transaction-starter</artifactId>
</dependency>
```

需要搭建独立 **Seata TC (Server)**、保证 Nacos/网络可达，在每个 AT 业务数据库创建对应版本官方 `undo_log`，且业务数据源必须由 Seata 自动代理。项目使用 Java 21、Boot 3.5.4、SCA 2025.0.0.0，其 BOM 对应 Seata 2.5.0。

```yaml
peach:
  transaction:
    enabled: true
    provider: seata
    mode: AT
    timeout-millis: 60000
seata:
  enabled: true
  tx-service-group: peach-cloud-tx-group
  registry:
    type: nacos
    nacos:
      server-addr: ${NACOS_SERVER_ADDR}
      application: seata-server
      group: SEATA_GROUP
  service:
    vgroup-mapping:
      peach-cloud-tx-group: default
```

生产须核对 Seata Server 部署、TC 组映射与官方配置中心参数。禁用时建议**同时**设置 `peach.transaction.enabled=false` 和 `seata.enabled=false`：后者用于关闭第三方 Seata SDK 自动初始化，避免仅关闭 Peach Facade 却仍启动 Seata。

业务短事务可直接使用官方 `@GlobalTransactional(rollbackFor = Exception.class)`；需通过 Spring 代理进入，不适用类内自调用。编程式 SPI 用法：

```java
TransactionOptions options = new TransactionOptions("update-account", TransactionMode.AT, 60000);
operations.execute(options, () -> {
    accountMapper.update(...);
    profileClient.update(...);
    return null;
});
```

上例业务调用为示意，请使用实际服务接口。现有 `@Transactional` 不能单独覆盖多服务；下游必须正确加入 XID，事务回滚条件和 Feign 异常必须真实向上抛出。

## 关键安全边界

- **AT 第一阶段已经提交本地事务**。如果同一服务使用 RocketMQ JDBC Outbox，Dispatcher 可能在全局事务回滚前发送消息；默认同时启用 `peach.rocket.outbox.enabled` 和 AT 时拒绝启动。仅在独立完成全局提交屏障并审查其持久化和崩溃恢复正确性后，才可设置 `peach.transaction.allow-at-outbox-coexistence=true`；该开关本身**不会实现屏障**。
- AT 不支持对 MinIO/OSS、MongoDB、Redis 与 MQ 进行数据库 Undo；这些场景使用现有可靠事件或另行实现持久化 Saga 补偿。
- 非幂等 Feign 写调用禁止盲目重试；Sentinel fallback 不可把失败伪装成成功；网关应剥离外部传入的 Seata XID，只允许受信任服务之间传播。当前 Starter 没有修改网关或 Feign 拦截器；部署前必须验证。
- `GlobalTransactionOperations` 使用当前线程上下文，异步或虚拟线程不自动继承 XID；已经加入的事务不由下游程序式方法提交。
- 事务 Provider 选取严格匹配名称且只允许一项；未知 Provider、未实现模式均直接失败。若需要按业务域分别配置不同模式，显式调用程序式 API，并先验证支持范围。

## 配置

| 属性 | 默认 | 说明 |
| --- | --- | --- |
| `peach.transaction.enabled` | `false` | 关闭 Peach Facade |
| `peach.transaction.provider` | `seata` | SPI Provider |
| `peach.transaction.mode` | `AT` | 唯一已实现模式 |
| `peach.transaction.timeout-millis` | `60000` | 默认超时毫秒 |
| `peach.transaction.allow-at-outbox-coexistence` | `false` | 默认防止未经治理的 AT/Outbox 混用 |

所有 Peach 属性见 `additional-spring-configuration-metadata.json`，Seata 属性使用第三方 Starter 的官方 metadata。

## 验证

```bash
mvn -pl peach-middleware/peach-transaction -am test -Pdevelopment
mvn -pl peach-middleware/peach-transaction/peach-transaction-quickstart -am spring-boot:run -Pdevelopment
```

Quickstart 默认禁用 Seata：只检查依赖闭包和应用启动。**不构成真实 AT 分支事务、Feign XID 传播或双 MySQL 回滚通过的证据。** 完整集成验收须启动 Seata Server/Nacos 与两个独立业务服务，对成功、分支异常、TC 故障、重试和回滚失败逐项注入验证。Seata 官方部署/建表指南：https://seata.apache.org/zh-cn/docs/v2.5/ops/deploy-guide-beginner/
