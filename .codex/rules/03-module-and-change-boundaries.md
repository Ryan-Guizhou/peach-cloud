# Module And Change Boundaries

本规则用于限制改动范围，避免 agent 破坏 `peach-cloud` 的模块边界。

## Module Placement

- `*-launch` 只放启动类和运行时配置
- `*-rest` 只放 Controller 和接口适配
- `*-service` 放领域服务和业务逻辑
- `*-entity` 放 DO、DTO、QO、VO 等模型
- `*-common` 只放当前业务域共享对象，不反向依赖上层模块
- `*-autoconfigure` 放 starter 核心 API、默认实现、自动配置、SPI
- `*-starter` 只做接入聚合，不承载复杂业务逻辑
- `*-example` 放可运行示例和覆盖默认实现的样例

## Project-Specific Boundaries

- `peach-common` 只承载通用响应、异常、常量、工具、上下文、基础模型，不放业务域逻辑
- RocketMQ 接入优先走 `MqPublisher`、`@MqEvent`、`@MqConsumer`、`MqMessageHandler<T>`
- Storage 接入统一走 `StorageTemplate`，不直接耦合厂商 SDK
- Threadpool 异步统一走 `ThreadPoolManager` 或 `@AsyncExecuted`，不要新建游离线程池
- `peach-cloud-front` 是独立 Vue 3 + Vite + TypeScript 工程，不加入 Maven reactor；前端改动使用 npm 脚本验证
- `peach-generator` 属于高影响代码生成模块，改模板、生成规则或 SQL 元数据处理前先确认生成产物影响范围

## Change Scope

- 默认只改用户要求范围内的文件
- 遇到公共 API、公共响应对象、基础实体、starter 配置类时，先评估影响面再改
- 文档任务只改文档；代码任务只改必要代码、测试和相关文档
- 不回退用户已有改动，不顺手做无关重构
- 当用户一次要求一组同类 controller 或 service 时，必须一次性完成整组，不允许只落地单个文件后就停
- 当 controller、service、DTO、group 之间存在联动时，必须同步更新对应层级的签名、校验和注释，避免只改表层 controller
- 默认采用单 DTO + JSR-303 分组的方式表达新增/更新场景，只有用户明确要求拆分 DTO，或者仓库既有范式已经是拆分结构时，才创建 AddDTO / UpdateDTO
- 编辑文件时默认使用增量修改，不要先整文件删除再重写；只有当结构性重构、跨段大改或增量补丁会显著降低可读性时，才允许全量替换
