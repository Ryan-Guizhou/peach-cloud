# peach-code

[English](README.en-US.md) | 中文

## 模块定位

`peach-code` 提供按“租户 + 编码前缀”隔离的业务编码生成能力，例如：

```text
T001 + MENU   -> MENU_00000001
T001 + NOTICE -> NOTICE_00000001
T002 + MENU   -> MENU_00000001
```

默认实现参考 `BizIdGenerator` 采用 Redis 优先、MySQL 兜底：Redis 使用 Lua 原子校准和递增，
Redis 不可用时使用 MySQL 原子自增；两端恢复时取最大值同步，避免序号回退。关闭 Redis 后，
也可以完全使用 MySQL 兜底模式。

本模块不负责自动给业务表增加 `TENANT_ID`，也不负责从业务表 `MAX(code)` 推断序号。

## 模块结构

```text
peach-code/
├── peach-code-autoconfigure  # CodeGenerator、JdbcTemplate 实现、自动配置
├── peach-code-starter        # 业务模块接入依赖
├── peach-code-example        # 可运行示例与接入边界示例
└── README.md
```

## 快速接入

业务模块依赖 starter：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-code-starter</artifactId>
</dependency>
```

先执行 `sql/PEACH_CODE_RULE.sql`，为每个租户配置允许使用的前缀：

```sql
INSERT INTO PEACH_CODE_RULE
    (TENANT_ID, CODE_PREFIX, MAX_CODE_WIDTH, CURRENT_VALUE, STATUS)
VALUES
    ('T001', 'NOTICE', 8, 0, 'ENABLE');
```

业务写入和生成编码必须处在同一个事务：

```java
@Transactional(rollbackFor = Exception.class)
public void saveNotice(String tenantId, NoticeDO notice) {
    notice.setNoticeCode(codeGenerator.next(tenantId, "NOTICE"));
    noticeDao.insert(notice);
}
```

`CodeGenerator.next` 没有活动事务时会直接拒绝执行，避免只提交序列表而未提交业务数据。

## 配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.code.enabled` | `true` | 是否启用自动配置 |
| `peach.code.redis-enabled` | `true` | 是否启用 Redis 优先发号及 MySQL 故障回写 |
| `peach.code.redis-key-prefix` | `peach:code:committed:` | Redis 序列 key 前缀 |

Redis key 还会拼接租户和前缀，例如：

```text
peach:code:committed:T001:NOTICE
```

完整业务数据不会写入 Redis，也不会写入日志。

## 运行机制

```text
业务调用
  -> 查询 PEACH_CODE_RULE 当前序号和最大宽度
  -> Redis Lua：以 MySQL 序号校准后原子递增
  -> 按 MAX_CODE_WIDTH 补零
  -> 写入业务表

Redis 不可用时
  -> MySQL LAST_INSERT_ID(CURRENT_VALUE + 1) 原子递增
  -> 独立事务提交
  -> Lua 单调回写 Redis
```

`MAX_CODE_WIDTH` 表示数字部分的最大长度，不是固定截断长度：

```text
MAX_CODE_WIDTH=8, value=1  -> 00000001
MAX_CODE_WIDTH=8, value=12 -> 00000012
MAX_CODE_WIDTH=8, value=100000000 -> 拒绝，序号超出配置
```

## 并发、重试与空号边界

- Redis Lua 按单个租户和前缀原子递增，不同租户/前缀互不阻塞。
- Redis 主路径的序号不参与业务事务，业务事务回滚后不会回收该序号，因此可能产生空号。
- 如果业务严格要求回滚不产生空号，应关闭 `peach.code.redis-enabled`，使用 MySQL 模式，
  并在业务 Service 事务中完成编码生成和业务写入。
- 业务数据删除不会回收编码，避免旧编码再次使用。
- Redis 丢失时会以 MySQL 当前序号重新校准；Redis 故障时自动降级 MySQL，恢复后单调回写 Redis。
- 重试必须重试整个带 `@Transactional` 的业务 Service 方法，不能只重试 `next()`。
- 发生数据库故障恢复、人工修改序列或外部非事务写入时，仍可能产生空号；模块优先保证不重复。

当前 `PEACH_MENU`、`PEACH_NOTICE` 尚未增加租户字段，因此本模块只提供租户维度的通用发号能力。后续业务表改造后，应增加对应的租户字段和联合唯一索引。

## 扩展与覆盖

自动配置只在容器中不存在 `CodeGenerator` 时创建默认 `PeachCodeGenerator`。业务可以提供自己的 `CodeGenerator` Bean 覆盖默认实现，但必须保留租户隔离、事务和唯一性约束。

## 示例与验证

示例模块为 `peach-code-example`。应用就绪后，`PeachCodeEvent` 会调用 `ExampleCodeService`
生成 MENU 和 NOTICE 编码，并在日志中输出编码及格式校验结果。

```bash
mvn -pl peach-component/peach-code -am compile -Pdevelopment
mvn -f peach-component/peach-code/peach-code-example/pom.xml spring-boot:run
```

启动示例前，需要准备数据库并执行 `sql/PEACH_CODE_RULE.sql`，并配置 Redis 密码。
`peach.code.example.tenant-id` 默认使用 `T001`；设置
`peach.code.example.verify-on-startup=false` 可关闭启动验证。示例不会自动创建生产数据库资源。

## 排障

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `CodeGenerator` 未注入 | 没有 `JdbcTemplate` 或自动配置被关闭 | 检查 JDBC starter、数据源和 `peach.code.enabled` |
| `No transaction manager available for MySQL fallback` | 没有配置事务管理器 | 为数据源配置 Spring 事务管理器 |
| `Code rule does not exist` | 租户和前缀未配置 | 插入对应的 `PEACH_CODE_RULE` 规则 |
| 序号超出最大长度 | `CURRENT_VALUE` 已超过 `MAX_CODE_WIDTH` | 扩大 `MAX_CODE_WIDTH`，不要回退 `CURRENT_VALUE` |
| Redis 序号没有更新 | Redis 不可用或被关闭 | 检查 Redis 连接；系统会自动使用 MySQL 兜底 |

## 版本与兼容性

模块按项目 Java 8、Spring Boot 2.7.13 和 Spring Data Redis 版本编写。数据库 SQL 使用 MySQL 的行锁语义。
