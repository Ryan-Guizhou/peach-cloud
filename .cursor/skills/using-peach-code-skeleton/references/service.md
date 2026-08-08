# Service Layer

Service 表达业务能力、事务边界和领域编排。当前实现只用于确认术语与兼容约束；异常、事务和安全设计必须按目标规则判断。

## 导航结构

```text
peach-cloud/
├── peach-auth/peach-auth-service/src/main/java/com/peach/auth/
│   ├── service/
│   │   ├── IUserService.java             # 公开业务能力
│   │   └── impl/UserServiceImpl.java     # 领域编排实现
│   └── dao/UserDao.java                  # 持久化边界
├── peach-fileservice/peach-fileservice-service/src/main/java/com/peach/fileservice/
│   └── service/impl/FileDomainServiceImpl.java
└── peach-generator/peach-generator-service/src/main/java/com/peach/generator/
    └── service/engine/                   # 生成器领域引擎
```

## REQUIRED

- 接口描述业务能力、输入输出和边界；Controller 不承担其业务逻辑。
- 写操作、跨 DAO 状态变化和一致性流程在可被 Spring 代理的公开方法建立事务边界。
- 禁止依赖 `private` 方法或 `this.xxx()` 自调用触发事务、缓存、异步和权限代理。
- REST 校验后继续执行权限、状态、唯一性、幂等和业务约束校验。
- 主键、租户、审计、逻辑删除、权限和敏感字段由服务端显式赋值，不信任 DTO。
- 外部调用、消息、缓存与数据库组合时明确失败顺序、补偿和最终一致性边界。
- 抛出可识别的业务/领域异常，不用裸 `RuntimeException` 掩盖错误类型。

## PREFERRED

- 必需依赖使用构造器注入；维护存量类时可继续一致使用 `@Resource`，但不在同一类混用。
- 公开方法保留可读主流程，局部组装使用 `build/require/resolve/validate` 等私有方法。
- DTO→DO 可以使用 `BeanUtils.copyProperties`，随后显式覆盖安全与审计字段。
- 返回显式 VO/结果对象，不把持久化 DO 直接交给 REST。
- 只读查询不默认加事务；确需一致性快照时说明原因和 `readOnly` 语义。

## LEGACY_COMPATIBLE

- `IUserService` 式 `I` 前缀、`pageList/add/delById` 等命名在既有模块内兼容。
- `@Resource`、`PageHelper`、`PageInfo/PageResult` 可按当前模块保持一致。
- `@Slf4j`、`@Indexed` 只在实际需要时保留，不机械复制固定注解组合。

## FORBIDDEN

- Service 处理 HTTP 参数绑定或直接返回 Controller 响应对象。
- `new Thread`、游离线程池、未关闭资源或吞掉 Future/异步异常。
- 在日志中输出完整 DTO、凭据、签名 URL 或敏感消息体。
- 用静态可变字段保存业务状态或请求上下文。
- 为复用把单一流程 helper 提前上提到 `peach-common`。

## 验证

- 事务方法是否可代理，异常是否触发预期回滚。
- DTO→DO→VO 是否阻断敏感字段。
- 外部副作用是否具备幂等或补偿。
- 运行受影响模块测试、`node scripts/check-utf8.mjs` 和 `git diff --check`。
