> 目的：约束 AI 在本仓库实现功能时，代码风格、分层边界、命名方式与现有工程保持一致，降低返工成本。
> 生成代码优先遵循本规范；若与现有代码冲突，以"保持现有模块风格一致"为最高优先级。

## 1. 适用范围
- 仓库根目录下全部 Java 微服务模块（`peach-auth`、`peach-monitor`、`peach-fileservice`、`peach-setting` 等）。

## 2. 技术基线
- JDK：`1.8`（禁止 Java 9+ 语法：`var`、`List.of`、文本块、`record`）
- Spring Boot：`2.7.x`
- Spring Cloud：`2021.0.5` / Alibaba `2021.0.5.0`
- MyBatis：`2.3.1`（原生 XML，不用 MyBatis-Plus）
- knife4j-openapi3：`4.4.0`

## 3. 主键与时间字段规范

### 3.1 主键（UUID）
- **所有 DO 的主键必须为 String 类型（UUID）**
- **禁止使用自增主键**（如 MySQL AUTO_INCREMENT）
- 主键生成方式：在 Service 层使用 `UUID.randomUUID().toString()` 或封装工具类

### 3.2 时间字段
- Java 端：统一使用 `String` 类型
- 数据库端：统一使用 `VARCHAR` 类型
- 时间格式：`yyyy-MM-dd HH:mm:ss`（如 `2026-03-21 10:30:00`）
- 字段名：`createTime` / `modifyTime`（继承自 `PeachDO`）

### 3.3 数据库兼容（databaseIdProvider）
- 当 SQL 需要使用数据库特定函数（如时间比对）时，必须使用 `databaseIdProvider` 做区分
- 例如：MySQL 使用 `NOW()`，Oracle 使用 `SYSDATE`
- 配置示例：
  ```xml
  <select id="selectByTimeRange" databaseId="mysql">
      SELECT ... WHERE CREATE_TIME &gt;= #{startTime}
  </select>
  <select id="selectByTimeRange" databaseId="oracle">
      SELECT ... WHERE CREATE_TIME &gt;= TO_DATE(#{startTime}, 'YYYY-MM-DD HH24:MI:SS')
  </select>
  ```

## 3. 模块结构与包路径

### 3.1 模块拆分
- `*-entity`：DO/DTO/QO/VO
- `*-common`：常量、枚举、工具
- `*-service`：业务实现、DAO、配置
- `*-rest`：内部 HTTP 接口
- `*-rest-external` / `*-openfeign-external`：对外服务间调用
- `*-launch`：启动类、健康检查

### 3.2 包路径
- 根包：`com.peach.<module>`
- 子包：`rest`、`service`、`service.impl`、`dao`、`config`、`entity`、`dto`、`qo`、`vo`、`constant`、`enums`、`utils`
- 禁止跨模块复制包名

## 4. 命名规范

### 4.1 类命名
| 类型 | 规则 | 示例 |
|------|------|------|
| Controller | `XxxController` | `FileController` |
| Service 接口 | `IXxxService` | `IFileService` |
| Service 实现 | `XxxServiceImpl` | `FileServiceImpl` |
| DAO | `XxxDao` | `IndexDao` |
| DO | `XxxDO extends BaseDO` | `FileIndexDO` |
| DTO | `XxxDTO`（独立类） | `FileIndexDTO` |
| QO | `XxxQO extends PeachEntity` | `FileIndexQO` |
| VO | `XxxVO extends XxxDO` | `FileIndexVO` |
| 分组 | `XxxGroup extends BaseGroup` | `FileIndexGroup` |
| Feign | `XxxFeignClient` | `AuthFeignClient` |
| 常量 | `XxxConst` / `XxxConstant` | `ServiceNameConstant` |
| 配置 | `XxxConfig` | `RedisConfig` |

### 4.2 方法与变量
- 方法名：小驼峰动词开头（`login`、`selectById`、`queryPage`）
- 变量名：小驼峰，语义明确，禁止 `tmp`、`obj`、`data1`
- 常量名：`UPPER_SNAKE_CASE`（`USER_SERVICE`、`FILE_PATH_SERVICE`）

## 5. Entity 层规范

### 5.1 PeachDO（DO基类）
- 所有 DO 类必须继承 `PeachDO`
- `@Data`，`implements Serializable`，声明 `serialVersionUID`
- 已包含 8 个公共审计字段，**设计数据库表时不得再创建这些字段**：
  - `createTime`、`createUserId`、`createUserCode`、`createUserName`
  - `modifyTime`、`modifyUserId`、`updateUserCode`、`updateUserName`
- **时间字段为 String 类型，数据库对应 VARCHAR**（格式 `yyyy-MM-dd HH:mm:ss`）
- 字段使用 `@Column(name = "中文列注释")`

### 5.2 XxxDO（实体）
- 继承 `PeachDO`，标注 `@Data @Entity @Table(name="表名") @Schema`
- 主键标注 `@Id`，**类型必须为 String（UUID），禁止自增**
- **只需定义业务字段**，审计字段从 PeachDO 继承

### 5.3 XxxVO（视图对象）
- 继承 `XxxDO`，添加 `@JsonInclude(JsonInclude.Include.NON_NULL)`
- 扩展前端展示所需字段

### 5.4 XxxDTO（传输对象）
- 独立类（不继承 DO），`@Schema` 描述字段
- 分组校验：insert 时 ID `@Null`，update 时 ID `@NotNull`
- 字段加 `@NotBlank`/`@Size` 等约束 + `groups`

### 5.5 XxxQO（查询对象）
- 继承 `PeachEntity`（分页基类，含 `pageNum`/`pageSize`）
- 添加查询条件字段

### 5.6 分组校验
- `BaseGroup`：`insertGroup`/`updateGroup`/`deleteGroup`/`queryGroup`
- 业务分组继承 `BaseGroup` 按需扩展
- Controller 使用 `@Validated(XxxGroup.insertGroup.class)`

## 6. Controller 规范

### 6.1 注解组合（缺一不可）
`@Tag(name, description)` + `@Slf4j` + `@Indexed` + `@RestController` + `@RequestMapping`

### 6.2 方法规范
- `@Operation(summary = "...")` — 必须
- 参数显式注解：`@PathVariable`/`@RequestParam`/`@RequestBody`/`@RequestPart`
- `@Validated(Group.class)` — 触发分组校验
- `@UserOperLog(...)` — 写操作记录操作日志
- 统一返回 `Response`,`Response`不需要带泛型,（文件流下载可返回 `ResponseEntity<StreamingResponseBody>`）

### 6.3 路由约定
- 内部接口：`/xxx`、`/xxx/common`
- 外部调用：`rest.external` 包，路径 `/<service>/external/...`
- 新增接口增量添加路径，禁止修改已有路径语义

## 7. Service 规范
- 接口 `IXxxService`，方法返回 `Response`,`Response`不需要带泛型
- 实现类：`@Slf4j` + `@Indexed` + `@Service`
- 依赖注入：**业务 Bean → `@Resource`**；**Spring/第三方 Bean → `@Autowired`**

## 8. DAO 与 MyBatis 规范

### 8.1 DAO 接口
- 继承 `PeachDao<DO, VO>`，标注 `@Indexed` + `@MybatisDao`
- 必须实现 `selectByQO` 方法

### 8.2 Mapper XML
- `namespace` = DAO 全限定名
- 必须包含 SQL 片段：`allColumn`、`allColumnAlias`、`allColumnValue`、`itemAllColumnValue`、`allColumnSet`、`updateSelectiveColumn`、`updateSelectiveValue`、`allColumnCond`
- 字段对齐排列（每行 4 列），所有字段指定 `jdbcType`
- 复用 `<include refid="..."/>`，用 `<where>` 包裹条件，`<trim>` 处理逗号
- 禁止 `SELECT *`
- 新增 SQL 优先复用已有通用方法

## 9. OpenFeign 规范
- 通过 `ServiceNameConstant` + `ServicePathConstant` 声明服务名和路径
- 方法参数必须带 Spring MVC 注解
- Feign 仅依赖 external/openfeign 模块

## 10. 响应与异常规范

### 10.1 统一响应
| 方法 | 场景 |
|------|------|
| `Response.success()` / `success(data)` | 成功 |
| `Response.fail()` / `fail(msg)` / `fail(StatusEnum)` | 失败 |
| `Response.paramError()` / `paramError(msg)` | 参数错误 |
| `Response.businessResponse(msg)` / `businessResponse(code, msg)` | 业务异常 |
| `Response.commonResponse(boolean)` | 根据布尔返回 |

状态码通过 `StatusEnum` 管理，禁止硬编码。

### 10.2 异常处理
- 业务异常：`BusinessException` / `LockException`
- 禁止吞异常，至少记录 message + stacktrace
- 全局由 `GlobalExceptionHandler` 兜底

## 11. 日志规范

### 11.1 基本规则
- 统一 `@Slf4j`，禁止 `System.out.println`
- 分级：`info`（关键流程）、`warn`（可恢复异常）、`error`（失败）
- 禁止输出密码、token、密钥、完整隐私数据

### 11.2 日志输出格式
- 入参日志使用 `JSON.toJSONString()` 序列化对象：
  ```java
  log.info("入参->{}", JSON.toJSONString(queryQO));
  ```
- 简单参数使用 `->{}` 模式：
  ```java
  log.info("id is ->{}", id);
  ```
- 方法入口记录入参，关键分支记录中间状态，异常记录完整堆栈：
  ```java
  log.error("处理失败, id->{}", id, e);  // 注意最后带异常对象
  ```

## 12. 常量与枚举规范
- 通用常量放 `peach-common`，模块私有放 `common/constant`
- 禁止硬编码"魔法值"
- 枚举最少包含 `code` + `message`
- 配置类只做 Bean/框架配置，禁止写业务逻辑

## 13. 禁止事项
1. Java 9+ 语法
2. `System.out.println`
3. 硬编码魔法值
4. Controller 写业务逻辑
5. 吞异常
6. `SELECT *`
7. 跨模块复制包名
8. 绕过 common 重复造轮子
9. 无关大规模重构
10. 修改已有接口 URL、返回结构、常量语义
11. **使用自增主键**（必须使用 UUID String 类型）
12. **使用 Date/LocalDateTime 类型**（时间字段必须为 String）


## 14. 自检清单
1. 包路径在正确模块下
2. 命名符合 Controller/IService/ServiceImpl/Dao/DO/DTO/QO/VO 规范
3. Controller 补齐 @Tag/@Operation，参数注解完整
4. 返回体统一 Response
5. 常量和状态码复用已有定义
6. Feign 使用常量声明
7. 无 Java 9+ 语法
8. 异常处理完整，日志规范
9. Mapper XML 规范
10. 无无关重构和破坏性变更
