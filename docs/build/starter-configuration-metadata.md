# Starter 配置元数据维护规范

[English](starter-configuration-metadata.en-US.md) | 中文

## 为什么要维护

在 IDE 中添加 `peach-*-starter` 后，应能通过 Spring Boot 配置提示看到属性名称、类型、用途以及可静态确定的默认值；缺少配置处理器或 `additional-spring-configuration-metadata.json` 会导致提示残缺。配置提示**不是**运行环境已有该 Bean 或外部服务可连接的保证。

## 项目约定

1. 每个 `peach-component`、`peach-middleware` 下的 `*-autoconfigure` 项目都必须携带 `src/main/resources/META-INF/additional-spring-configuration-metadata.json`，由 `*-starter` 间接带入 Classpath。没有公开配置项时允许 `{"properties":[],"hints":[]}`。
2. 声明 `@ConfigurationProperties` 的模块还须引入 `spring-boot-configuration-processor`（`optional=true`），由编译器生成 `META-INF/spring-configuration-metadata.json`。手工 additional metadata 用于补齐描述、字典提示、嵌套 Map/列表及运行配置默认值，不要把配置处理器生成的文件手动提交到源码。
3. 新增字段时同步更新 additional metadata；非敏感静态默认值必须与 Java 初始化值一致。秘钥、密码、地址等无默认值时不应编造占位值，描述必须说明由安全配置提供。使用常量、构造逻辑、Nacos 覆盖或运行时计算决定的默认值，应在描述中解释来源，不能把本地假设写成全局事实。
4. 属性一律使用 canonical kebab-case，嵌套组按 `peach.xxx.group.property`、动态 Map 按 `peach.xxx.providers.*.endpoint` 描述。不要在多个 additional 文件声明互相矛盾的默认值。
5. `@ConditionalOnProperty`、`@Value` 等未通过 `@ConfigurationProperties` 自动生成的项目公开配置，由开发者明确列在 additional metadata 中；无需配置的 Starter 保留空清单。

## 防回归门禁

```bash
node scripts/check-spring-configuration-metadata.mjs
node scripts/verify-changes.mjs
```

门禁验证所有 AutoConfigure 文件存在、JSON 结构合法、属性唯一且名称/类型/描述合法、声明的配置字段没有遗漏，以及可以静态推断的默认值与 Java 定义没有漂移。针对嵌套类和同包的配置对象也覆盖；还检查 `spring-boot-configuration-processor` 依赖。

快速补齐（会生成模板描述，需人工检查后提交）：

```bash
node scripts/check-spring-configuration-metadata.mjs --fix
```

`--fix` 无法可靠推断动态常量、外部配置覆盖、构造器条件默认值；合并前由开发者和 Review 审核。

## 验收

`mvn -B -ntp verify` 必须通过；在 Starter 对应 Quickstart 或业务项目的 IntelliJ IDEA Spring Boot 配置编辑器中验证属性补全与默认值显示。每个模块的生成元数据和 additional metadata 应位于其构建 JAR 的 `META-INF` 下。IDE 的提示展示取决于 Spring Boot 插件、依赖索引及编辑器刷新状态，不能仅凭源码 JSON 推断用户界面已刷新。
