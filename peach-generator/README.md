# peach-generator

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-generator`  
类型：代码生成业务域聚合模块

## 模块定位

`peach-generator` 提供数据源管理、元数据读取、模板配置、生成配置、代码预览和代码生成能力。它面向开发期或平台内低代码能力，帮助根据数据库表和模板生成后端或前端代码。

本模块解决：

- 数据源配置和连接管理。
- 数据库元数据读取和字段映射。
- 代码生成模板、生成配置和预览接口。
- 生成器服务启动和 REST API。

本模块不解决：

- 任意数据库方言的完整兼容。
- 生成代码后的业务正确性保证。
- 生产数据库的无风险直连和变更治理。
- 模板安全沙箱和恶意模板隔离。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-generator-common` | 生成器公共对象和常量 |
| `peach-generator-entity` | 数据源、元数据、模板、生成配置等实体模型 |
| `peach-generator-service` | 元数据读取、模板处理和生成逻辑 |
| `peach-generator-rest` | 生成器 REST 接口 |
| `peach-generator-launch` | Spring Boot 启动模块 |

## 关键入口

| 类型 | 路径 |
| --- | --- |
| 启动类 | `peach-generator-launch/src/main/java/com/peach/generator/launch/PeachGeneratorApplication.java` |
| 配置文件 | `peach-generator-launch/src/main/resources/application-dev.yml` |
| REST 包 | `peach-generator-rest/src/main/java/com/peach/generator/rest/internal` |
| 服务包 | `peach-generator-service/src/main/java/com/peach/generator/service` |
| SQL 脚本 | `sql/PEACH_GENERATOR.sql` |

## REST 能力

| 控制器 | 路径前缀 | 说明 |
| --- | --- | --- |
| `GenDatasourceController` | `/generator/datasource` | 数据源配置 |
| `GenMetadataController` | `/generator/metadata` | 表和字段元数据 |
| `GenTemplateController` | `/generator/template` | 生成模板 |
| `GenConfigController` | `/generator/config` | 生成配置 |
| `GenCodeController` | `/generator/code` | 代码生成和预览 |

## 运行机制

1. 维护数据源配置，连接目标数据库。
2. 读取表、列、索引等元数据并转换为生成模型。
3. 按生成配置选择模板和输出规则。
4. 渲染模板，返回预览内容或生成文件。
5. 生成结果需要人工 review 后再进入业务仓库。

## 配置说明

- 启动配置位于 `peach-generator-launch/src/main/resources/application-*.yml`。
- 生成器数据库结构位于 `sql/PEACH_GENERATOR.sql`。
- 数据源连接信息属于敏感配置，不应提交真实生产账号密码。
- 模板路径、输出路径和包名规则应按项目约定配置。

## 边界与限制

- 生成器输出只是初始代码，不替代领域建模、接口设计和安全审查。
- 连接生产数据库前需要确认只读权限、网络隔离和审计策略。
- 模板中不要写入生产密钥、真实内网地址或不可公开业务规则。
- 覆盖已有文件前应做 diff 和备份，避免误删手写逻辑。

## 构建与验证

```bash
mvn -f "peach-generator/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-generator/peach-generator-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-generator/peach-generator-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 数据源连接失败 | JDBC URL、账号、密码、网络、驱动是否正确 | 使用只读账号先验证连接，再检查服务日志 |
| 元数据读取为空 | 表名、schema、权限是否正确 | 确认数据库用户可读取目标 schema 元数据 |
| 模板渲染失败 | 模板变量、字段命名、空值处理是否匹配 | 检查模板和生成模型字段 |
| 生成内容覆盖错误 | 输出路径和文件名规则是否正确 | 先使用预览和 diff，再执行写入 |
| 生成器表缺失 | `sql/PEACH_GENERATOR.sql` 是否执行 | 初始化数据库并确认表结构版本 |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
