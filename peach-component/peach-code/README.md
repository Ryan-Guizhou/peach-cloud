# peach-code

[English](README.en-US.md) | 中文

`peach-code` 按「租户 + 编码前缀」隔离生成业务编码，例如：

```text
T001 + MENU   -> MENU_00000001
T001 + NOTICE -> NOTICE_00000001
T002 + MENU   -> MENU_00000001
```

默认实现 Redis 优先、MySQL 兜底：Redis 用 Lua 原子校准并递增；Redis 不可用时走 MySQL `LAST_INSERT_ID` 原子自增，恢复后单调回写 Redis。关闭 `peach.code.redis-enabled` 后可只用 MySQL 独立事务发号。

本模块不负责给业务表补 `TENANT_ID`，也不从业务表 `MAX(code)` 推断序号。

## 模块结构

```text
peach-code/
├── peach-code-autoconfigure  # CodeGenerator、PeachCodeGenerator、自动配置
├── peach-code-starter        # 业务模块接入依赖
├── peach-code-quickstart     # 内存模式可运行示例
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

先执行仓库 [`sql/PEACH_CODE_RULE.sql`](../../sql/PEACH_CODE_RULE.sql)，为每个租户配置允许使用的前缀（表含 `ORG_ID`，生成器按 `TENANT_ID + CODE_PREFIX` 读规则）：

```sql
INSERT INTO PEACH_CODE_RULE
    (TENANT_ID, ORG_ID, CODE_PREFIX, MAX_CODE_WIDTH, CURRENT_VALUE, STATUS)
VALUES
    ('T001', 'ORG001', 'NOTICE', 8, 0, 'ENABLE');
```

注入公开 API `CodeGenerator`：

```java
public void saveNotice(String tenantId, NoticeDO notice) {
    notice.setNoticeCode(codeGenerator.next(tenantId, "NOTICE"));
    noticeDao.insert(notice);
}
```

`CodeGenerator.next` 不要求当前存在业务事务。Redis 主路径已分配的序号不会随业务回滚回收；若必须避免回滚空号，关闭 `peach.code.redis-enabled`，并在同一 MySQL 事务中完成发号与业务写入。

## 配置

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.code.enabled` | `true` | 是否启用自动配置 |
| `peach.code.redis-enabled` | `true` | 是否启用 Redis 优先发号及 MySQL 故障回写 |
| `peach.code.redis-key-prefix` | `peach:code:generate:` | Redis 序列 key 前缀 |

Redis key 还会拼接租户和前缀（前缀转大写），例如：

```text
peach:code:generate:T001:NOTICE
```

完整业务数据不会写入 Redis，也不会写入日志。

## 运行机制

```text
业务调用 CodeGenerator.next
  -> 查询 PEACH_CODE_RULE 当前序号和最大宽度
  -> Redis Lua：以 MySQL 序号校准后原子递增
  -> 按 MAX_CODE_WIDTH 补零后返回

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
- Redis 主路径的序号不参与业务事务，业务回滚后不会回收该序号，因此可能产生空号。
- 如果业务严格要求回滚不产生空号，应关闭 `peach.code.redis-enabled`，使用 MySQL 模式，并在业务 Service 事务中完成编码生成和业务写入。
- 业务数据删除不会回收编码，避免旧编码再次使用。
- Redis 丢失时会以 MySQL 当前序号重新校准；Redis 故障时自动降级 MySQL，恢复后单调回写 Redis。
- 重试必须重试整个带 `@Transactional` 的业务 Service 方法，不能只重试 `next()`。
- 发生数据库故障恢复、人工修改序列或外部非事务写入时，仍可能产生空号；模块优先保证不重复。

当前 `PEACH_MENU`、`PEACH_NOTICE` 尚未增加租户字段，因此本模块只提供租户维度的通用发号能力。后续业务表改造后，应增加对应的租户字段和联合唯一索引。

## 扩展与覆盖

自动配置只在容器中不存在 `CodeGenerator` 时创建默认 `PeachCodeGenerator`。业务可以提供自己的 `CodeGenerator` Bean 覆盖默认实现，但必须保留租户隔离、事务和唯一性约束。

## 示例与验证

- 模块：[`peach-code-quickstart`](./peach-code-quickstart/)
- 默认内存模式：`peach.code.redis-enabled=false`，排除数据源/Redis 自动配置，注入公开 API `CodeGenerator`
- 能力样例：
  - **顺序发号**：同一租户 + `MENU` 连续 `next`，得到 `MENU_00000001`、`MENU_00000002`
  - **隔离**：`T001/MENU`、`T001/NOTICE`、`T002/MENU` 序列互不影响
  - **非法参数**：租户或前缀含不支持字符时抛出 `CodeGeneratorException`
- 启动后 `CodeDemoRunner` 依次执行；关闭演示：`quickstart.code.demo.enabled=false`
- 运行：`mvn -pl peach-component/peach-code/peach-code-quickstart -am spring-boot:run`
- 测试：`mvn -pl peach-component/peach-code/peach-code-quickstart -am test`

生产接入需准备 `PEACH_CODE_RULE`、数据源，以及可选的 Redis（`peach.code.redis-enabled=true`）。

## 排障

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `CodeGenerator` 未注入 | 没有 `JdbcTemplate` 或自动配置被关闭，且未提供自定义 Bean | 检查 JDBC starter、数据源和 `peach.code.enabled`，或自行注册 `CodeGenerator` |
| `No transaction manager available for MySQL fallback` | 没有配置事务管理器 | 为数据源配置 Spring 事务管理器 |
| `Code rule is missing or disabled` / `Failed to load code rule` | 租户和前缀未配置或已停用 | 插入对应的 `PEACH_CODE_RULE` 规则 |
| 序号超出最大长度 | `CURRENT_VALUE` 已超过 `MAX_CODE_WIDTH` | 扩大 `MAX_CODE_WIDTH`，不要回退 `CURRENT_VALUE` |
| Redis 序号没有更新 | Redis 不可用或被关闭 | 检查 Redis 连接；系统会自动使用 MySQL 兜底 |

## 版本与兼容性

模块按项目 Java 21、Spring Boot 3.5.4 和 Spring Data Redis 版本编写。数据库 SQL 使用 MySQL 的行锁语义。

## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 target/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
