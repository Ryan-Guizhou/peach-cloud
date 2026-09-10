# peach-code-quickstart

用于验证 `peach-code-starter` 接入、事务内生成业务编码以及 Redis/MySQL 运行条件。

## 接入

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-code-starter</artifactId>
</dependency>
```

先执行 `sql/PEACH_CODE_RULE.sql` 及对应初始化数据，再按 `src/main/resources/application.yml` 配置数据库和 Redis。启动类为 `com.peach.code.quickstart.PeachCodeQuickstartApplication`。

`QuickstartCodeService` 展示编码生成和校验，`PeachCodeEvent` 展示应用启动时的可选验证。生产业务应在 Service 事务中调用 `CodeGenerator`，不能把 quickstart 的启动验证直接复制到业务启动流程。

## 运行

```bash
mvn -f peach-component/peach-code/peach-code-quickstart/pom.xml spring-boot:run
```

quickstart 依赖包含 JDBC、Redis 和 MySQL 运行驱动，仅用于本地接入验证；业务应用只依赖 `peach-code-starter`。
