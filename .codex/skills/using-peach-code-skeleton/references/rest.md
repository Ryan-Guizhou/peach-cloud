# REST Layer

参考基线：

- `peach-auth/peach-auth-rest/src/main/java/com/peach/auth/rest/internal/UserController.java`
- `peach-auth/peach-auth-rest/src/main/java/com/peach/auth/rest/internal/LoginController.java`
- `peach-auth/peach-auth-rest/src/main/java/com/peach/auth/rest/external/RoleExternalController.java`
- `peach-message/peach-message-rest/src/main/java/com/peach/message/rest/external/MessageExternalController.java`
- `.codex/rules/07-comments-and-logging.md`

当前仓库里的 controller 有两类：

- `rest/internal`：面向本系统前端或内部业务接口，常见写操作日志 `@UserOperLog`、分组校验、分页查询。
- `rest/external`：面向其他服务暴露，通常更薄，少业务日志，但仍保留 `Response` 包装、Swagger 注解和必要校验。

通用规则：

- 类上优先沿用 `@Slf4j`、`@Indexed`、`@Validated`、`@RestController`、`@RequestMapping`、`@Tag`。
- 类注释使用 Javadoc，优先写业务域、职责边界、主要接口用途；不是所有存量代码都写得完整，但新代码应补齐。
- Swagger 统一使用 OpenAPI 3：类上 `@Tag`，方法上 `@Operation`，必要时参数上 `@Parameter`。
- 返回值统一使用 `Response`；不要直接返回裸对象、`PageInfo`、`List`。
- REST 层只做参数接收、分组校验、调用 service、响应转换；不要堆事务、复杂业务编排、DAO 调用、线程控制。

参数与路由规则：

- 查询分页当前仓库常见 `@PostMapping("/pageList") + @RequestBody QO`。
- 主键详情既有 `@GetMapping("/selectById")` + 普通参数，也有 `@GetMapping("/xxx/{id}")` + `@PathVariable`；优先跟随当前模块已有风格，不要跨模块统一强改。
- 写操作当前仓库多用 `@PostMapping("/add")`、`/update`，删除常用 `@DeleteMapping("/delById")` 或 `/delete`。
- JSR-303 校验可以落在参数位，例如 `@Validated(Group.class) @RequestBody DTO`、`@NotBlank String id`。

内部接口规则：

- 涉及新增、更新、删除、发布、撤销、导入等业务操作时，优先补齐 `@UserOperLog`。
- `@UserOperLog` 的 `moduleCode`、`optType`、`optLevel`、`optContent` 先对齐相邻 controller，避免新造表达方式。
- 分页接口通常直接 `return Response.success(service.pageList(qo));`。

外部接口规则：

- 放在 `rest/external` 包下，路径通常带 `/external` 前缀。
- 一般不加 `@UserOperLog`，除非当前模块已有明确先例。
- 仍需保留 `@Tag`、`@Operation`、必要的 `@Validated`。

日志规则：

- Controller 自身通常日志不多，除非需要记录边界异常或关键请求。
- 日志中不要打印密码、token、完整请求报文、签名 URL。
- 不要为了“有日志”强行在每个接口头尾打印一遍。

案例 1：内部 controller

```java
/**
 * 用户管理接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime yyyy/M/d HH:mm
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/auth/user")
@Tag(name = "UserController", description = "用户管理")
public class UserController {

    @Resource
    private IUserService userService;

    @Operation(summary = "查询用户列表")
    @PostMapping("/pageList")
    public Response pageList(@RequestBody UserQO userQO) {
        return Response.success(userService.pageList(userQO));
    }

    @Operation(summary = "新增用户")
    @PostMapping("/add")
    @UserOperLog(moduleCode = UserLogEnum.Module.USERSERVICE, optType = UserLogEnum.OptType.INSERT,
            optLevel = UserLogEnum.LogLevel.INFO, optContent = "'新增用户信息,用户信息:['+#p0+']'")
    public Response add(@Validated(UserGroup.insertGroup.class) @RequestBody UserDTO userDTO) {
        userService.add(userDTO);
        return Response.success();
    }
}
```

案例 2：外部 controller

```java
/**
 * 消息服务外部接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime yyyy/M/d HH:mm
 */
@Slf4j
@Indexed
@Validated
@RestController
@RequestMapping("/message/external")
@Tag(name = "消息服务外部接口", description = "消息服务外部接口")
public class MessageExternalController {

    @Resource
    private IMessageService messageService;

    @PostMapping("/publish")
    @Operation(summary = "发布消息")
    public Response publish(@Validated @RequestBody MessagePublishDTO data) {
        return messageService.publish(data);
    }
}
```

提交前检查：

- 当前类属于 `internal` 还是 `external`，注解和路径是否跟包位一致。
- 是否统一返回 `Response`。
- 写操作是否遗漏 `@UserOperLog`。
- 参数校验是否真实落在 controller 入参，而不是只在 DTO 上声明。
- 是否把业务判断、事务、DAO 调用错误地下沉到了 REST 层。
