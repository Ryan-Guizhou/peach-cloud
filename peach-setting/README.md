# peach-setting

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-setting`  
类型：系统配置业务域聚合模块

## 模块定位

`peach-setting` 提供字典、值集、通知、多语言消息等系统配置能力。它面向业务系统提供配置数据维护、查询和服务间调用入口。

本模块解决：

- 字典和值集数据管理。
- 通知公告类配置维护。
- 多语言消息配置维护。
- 系统配置服务启动、REST 接口和 OpenFeign 外部接口。

本模块不解决：

- Nacos 配置中心的配置发布和动态刷新治理。
- 业务服务所有配置项的集中托管。
- 国际化资源的前端渲染策略和客户端缓存策略。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-setting-common` | 系统配置域公共对象和常量 |
| `peach-setting-entity` | 字典、值集、通知、多语言等实体模型 |
| `peach-setting-service` | 系统配置领域服务和数据访问 |
| `peach-setting-rest` | REST 接口 |
| `peach-setting-openfeign-external` | 面向其他服务的 OpenFeign 接口 |
| `peach-setting-launch` | Spring Boot 启动模块 |

## 关键入口

| 类型 | 路径 |
| --- | --- |
| 启动类 | `peach-setting-launch/src/main/java/com/peach/setting/launch/PeachSettingApplication.java` |
| 配置文件 | `peach-setting-launch/src/main/resources/application-dev.yml` |
| REST 包 | `peach-setting-rest/src/main/java/com/peach/setting/rest/internal` |
| 服务包 | `peach-setting-service/src/main/java/com/peach/setting/service` |
| OpenFeign | `peach-setting-openfeign-external/src/main/java/com/peach/setting/openfeign` |

## REST 能力

| 控制器 | 路径前缀 | 说明 |
| --- | --- | --- |
| `DictController` | `/setting/dict` | 字典配置 |
| `ValueSetController` | `/setting/valueSet` | 值集配置 |
| `NoticeController` | `/setting/notice` | 通知配置 |
| `MultiMessageController` | `/setting/multiMessage` | 多语言消息配置 |

## 运行机制

1. `peach-setting-launch` 启动服务并加载环境配置。
2. REST 层接收配置维护和查询请求。
3. Service 层处理配置数据的校验、查询和持久化。
4. 其他服务通过 OpenFeign 模块查询配置数据。
5. 调用方按业务需要缓存字典、值集或多语言消息。

## 配置说明

- 启动配置位于 `peach-setting-launch/src/main/resources/application-*.yml`。
- 数据库、Nacos、Redis 等连接参数需要按 profile 配置。
- 字典和值集是否允许缓存、缓存多久，应由调用方结合业务一致性要求决定。

## 边界与限制

- 本模块管理业务配置数据，不替代 Nacos 这类应用配置中心。
- 字典和值集修改后的实时生效取决于调用方缓存策略。
- 多语言消息配置只提供数据来源，不负责前端选择语言和渲染。
- 外部接口需要服务间认证或网关保护，避免被未授权调用。

## 构建与验证

```bash
mvn -f "peach-setting/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-setting/peach-setting-launch -am clean package -DskipTests -Pdevelopment
mvn -pl peach-setting/peach-setting-launch -am -Dspring-boot.run.profiles=dev spring-boot:run
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 字典查询为空 | 初始化脚本、租户/应用维度、查询条件是否正确 | 检查数据库数据和请求参数 |
| 配置修改后未生效 | 调用方是否缓存；缓存是否刷新 | 清理调用方缓存或调整缓存策略 |
| 多语言消息缺失 | 语言编码、消息 key、默认语言是否存在 | 补齐配置记录并检查前端语言选择 |
| Feign 调用失败 | 服务注册、接口依赖、Nacos 是否正常 | 检查 `peach-setting-openfeign-external` 和服务状态 |
| 生产配置泄漏 | 是否把应用密钥当作业务配置维护 | 敏感配置应放入安全配置体系，不写入普通配置表 |
