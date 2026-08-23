# Module And Change Boundaries

## Module Placement

`REQUIRED`：

- `*-launch`：启动类和运行时配置。
- `*-rest`：Controller 和接口适配，不直接调用 DAO。
- `*-service`：领域逻辑、事务、状态流转和业务编排。
- `*-entity`：DO、DTO、QO、VO 等模型。
- `*-common`：当前业务域共享对象，不反向依赖上层模块。
- `*-openfeign-external`：跨服务 Feign 契约。
- `*-autoconfigure`：starter 核心 API、自动配置、默认实现和 SPI。
- `*-starter`：接入依赖聚合，不承载复杂业务逻辑。
- `*-example`：可运行示例和覆盖默认实现的样例。

业务模块优先依赖 starter；RocketMQ 使用 `MqPublisher`/`@MqConsumer`，Storage 使用 `StorageTemplate`，异步使用 `ThreadPoolManager`/`@AsyncExecuted`。`peach-common` 只承载无业务域语义的稳定基础能力。

## Change Scope

- 默认只修改用户要求所需代码、测试和行为相关文档，不顺手重构无关代码。
- 用户一次点名一组同类文件时，先扫描整组，再一次性完成；Controller、Service、DTO、校验分组、DAO/XML 联动必须同步。
- 公共 API、公共响应、基础实体、starter 配置、生成器模板和 DAO 签名改动前先评估影响面。
- 保留用户未提交改动；禁止通过删除重建、`git checkout --`、`git reset --hard` 等方式覆盖。
- 优先增量修改；批量机械编码处理只允许执行可审计的 UTF-8 BOM 移除，不得改变正文。

## Existing Style Versus Target Style

- 安全、正确性、Java 21 和模块边界是 `REQUIRED`。
- 单 DTO + JSR-303 分组是 `PREFERRED`；当前模块明确采用拆分 DTO 时可以兼容。
- 历史 CRUD 路由、VO 继承 DO、固定注解组合、接口命名等属于 `LEGACY_COMPATIBLE`，不得无条件扩散到新公共 API。
- 完整 DTO 日志、敏感字段响应、错误事务位置、资源泄漏和跨层依赖属于 `FORBIDDEN`，即使相邻代码存在也不能复制。
