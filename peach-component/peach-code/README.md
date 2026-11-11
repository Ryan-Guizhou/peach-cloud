# peach-code

[English](README.en-US.md) | 中文

`peach-code` 为每个 `TENANT_ID + CODE_PREFIX` 分配业务编码。当前唯一发号权威是 **MySQL**，不再在 Redis 和 MySQL 之间切换主路径。

## 模块与使用

| 模块 | 能力 |
| --- | --- |
| `peach-code-autoconfigure` | `CodeGenerator` API、MySQL 事务发号、Spring 自动配置 |
| `peach-code-starter` | 业务接入依赖 |
| `peach-code-quickstart` | 无数据库的内存示例，仅用于公开 API 演示 |

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-code-starter</artifactId>
</dependency>
```

部署前执行 [sql/PEACH_CODE_RULE.sql](../../sql/PEACH_CODE_RULE.sql) 创建规则表；每个租户和前缀必须唯一。使用 `CodeGenerator.next(tenantId, prefix)` 获取如 `MENU_00000001` 的编码。

## 发号与故障语义

```text
CodeGenerator.next
  -> MySQL REQUIRES_NEW 事务
  -> SELECT ... FOR UPDATE 锁定 TENANT_ID + CODE_PREFIX
  -> 校验 ENABLE、位数及溢出
  -> 条件 UPDATE CURRENT_VALUE
  -> COMMIT 后返回业务编码
```

- 单一 MySQL 数据源是发号权威。Redis 故障或恢复不会触发另一路发号，也不会造成序号回退。
- 同一租户 + 前缀的发号由 InnoDB 行锁串行化；不同租户和前缀可并发。
- 数据库不可用时抛异常，不自动降级到 Redis 或内存。调用失败但事务已提交可能产生空号；不能保证严格连续。
- 发号事务独立提交，业务回滚不回收编码。业务表仍需建立正确的唯一索引。
- `MAX_CODE_WIDTH` 范围为 1–18，溢出时不提交水位更新。
- `peach.code.enabled` 默认 `true`。历史 `peach.code.redis-enabled` 和 `peach.code.redis-key-prefix` 继续接受配置，但**不再改变发号路径**，后续配置应删除这些历史键。

## 从旧 Redis 主发号升级（必须执行）

**禁止旧 Redis 主发号实例与新 MySQL 唯一发号实例混合运行。**

1. 暂停业务发号和重试任务，等旧实例结束所有在途发号操作。
2. 备份 `PEACH_CODE_RULE` 及历史 Redis 序号；按租户 + 前缀核对 Redis 最大已分配值和 MySQL `CURRENT_VALUE`。
3. 将每条 MySQL 序号提升到不小于对应 Redis 历史最大值；不要将序号回退。并确认没有其他路径在直接修改计数器。
4. 执行重复规则检查：
   ```sql
   SELECT TENANT_ID, CODE_PREFIX, COUNT(*) AS COUNT_ROWS
   FROM PEACH_CODE_RULE
   GROUP BY TENANT_ID, CODE_PREFIX HAVING COUNT(*) > 1;
   ```
5. 清理重复规则并执行 `ALTER TABLE PEACH_CODE_RULE ADD UNIQUE KEY UK_PEACH_CODE_TENANT_PREFIX (TENANT_ID, CODE_PREFIX)`；新建数据库可直接使用当前建表 SQL。
6. 全部替换为新实现后，才恢复发号流量。旧版本不能再恢复运行，除非重新执行完整切换与水位校准。

旧表的 `ORG_ID` 是规则归属字段而非序号隔离维度；当前公开 API 不接受 `ORG_ID`。若同租户+前缀存在多个组织记录，必须先明确保留规则，不应自动合并。

## 验证与边界

- 运行：`mvn -pl peach-component/peach-code/peach-code-quickstart -am spring-boot:run`
- 测试：`mvn -pl peach-component/peach-code/peach-code-quickstart -am test`
- Quickstart 使用单进程内存发号器，不代表 MySQL 并发、故障或部署迁移已验证。生产验收需在 MySQL 中测试同键竞争、回滚、断库和溢出场景。
- 业务表租户约束、唯一索引和多组织规则清理由业务迁移负责；不要依赖本组件自动修改业务表。

遵循项目 Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0 与 Spring Cloud Alibaba 2025.0.0.0 基线；中文 Javadoc、英文日志、UTF-8 无 BOM。
