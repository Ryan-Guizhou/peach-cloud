# REST Layer

REST 是协议适配边界。当前 Controller 只用于确认已有契约；安全、校验和职责边界高于注解组合与路由形式。

## 导航结构

```text
peach-auth/peach-auth-rest/src/main/java/com/peach/auth/rest/
├── internal/
│   ├── UserController.java               # 本系统业务接口
│   └── LoginController.java              # 认证边界
└── external/
    └── RoleExternalController.java       # 跨服务外部接口
```

处理接口时继续向下定位 `peach-auth-entity` 的 DTO/QO/VO 和 `peach-auth-service` 的 Service 契约。

## REQUIRED

- Controller 只做参数绑定、JSR-303 校验、鉴权边界、Service 调用和响应转换。
- 主键、查询和路径参数显式使用 `@RequestParam`、`@PathVariable` 等绑定注解，不依赖参数名推断。
- 请求体按场景使用 `@Validated(Group.class)`；类级 `@Validated` 用于方法参数约束。
- internal/external 路径、权限和暴露范围必须与包职责一致。
- 返回模型逐字段检查敏感数据；不得返回含 password/token/secret 的 DO/VO。
- 操作审计只记录经确认的非敏感字段白名单，不引用完整 DTO、请求或对象 `toString()`。

## PREFERRED

- 新公共 API 使用语义清晰、稳定且可演进的资源/动作设计；错误响应由统一异常处理生成。
- `@Tag`、`@Operation` 和参数文档描述真实契约，不复制类型名作为无意义说明。
- `@Slf4j` 仅在实际记录边界日志时添加；日志级别按结果语义选择。
- 简单转发可以直接返回 `Response.success(service.xxx(...))`，但复杂转换应使用明确 mapper/assembler。

## LEGACY_COMPATIBLE

- `/pageList`、`/selectById`、`/add`、`/update`、`/delById` 等动作式路由在维护既有 API 时保留。
- 当前非泛型 `Response` 和 internal/external 注解组合属于兼容约束，不作为新公共响应设计的质量证明。
- `@Indexed` 仅在确有组件索引需求时保留，不机械添加。

## FORBIDDEN

- Controller 直接调用 DAO、开启事务、创建线程或实现核心业务流程。
- `@UserOperLog` 使用 `#p0` 等表达式记录整个 DTO。
- DELETE 固定映射 ERROR、UPDATE 固定映射 DEBUG 等机械日志级别规则。
- 仅因相邻 Controller 存在就复制无效注解、缺失绑定或敏感响应。

## 验证

- 检查绑定来源、校验分组、权限、Service 签名、响应模型和操作日志。
- 操作日志 SpEL 必须结合当前解析器源码验证，不提供未经验证的表达式模板。
- 运行接口/模块测试、UTF-8 检查和 `git diff --check`。
