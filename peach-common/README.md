# peach-common

[English](README.en-US.md) | 中文

最后更新时间：2026-09-08
artifactId：`peach-common`  
类型：公共基础模块

## 模块定位

`peach-common` 是后端模块共享的基础依赖，承载无业务域语义的稳定契约和工具能力。业务模块、组件模块和中间件模块可以依赖它，但它不能反向依赖具体业务域、Web 运行时、数据库访问、Redis 客户端、Sa-Token 实现或厂商 SDK。

本模块解决：

- 统一响应、异常、分页和基础实体契约。
- 跨模块复用的字符串、集合、时间、脱敏、加解密等基础工具。
- 审计上下文的最小读取契约，由具体安全模块提供实现。
- SPI 形式的对称加解密扩展入口。

本模块不解决：

- 具体业务流程、业务缓存 key、Topic、表字段和领域状态码。
- Web Controller、全局异常处理器、过滤器、拦截器和自动配置。
- Redis、MQ、Storage、Sa-Token 等中间件的运行时装配。
- token、私钥、签名 URL 或业务生产凭据的默认配置。

## 目录结构

```text
peach-common
├── pom.xml
├── README.md
├── README.en-US.md
└── src
    ├── main
    │   ├── java/com/peach/common
    │   └── resources
    └── test
        └── java/com/peach/common
```

`target/`、`.flattened-pom.xml`、IDE 缓存和依赖缓存不是源码结构的一部分。

## Maven 接入

业务模块通过 Maven 依赖使用：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-common</artifactId>
</dependency>
```

仓库内版本由根 POM 的 `dependencyManagement` 统一管理，通常不需要在业务模块中显式写版本。

## 核心能力

| 包路径 | 职责 |
| --- | --- |
| `com.peach.common.response` | 统一响应模型和状态码 |
| `com.peach.common.exception` | 公共异常类型和校验异常详情 |
| `com.peach.common.audit` | 审计上下文读取契约、快照和基础实体填充工具 |
| `com.peach.common.loader` | 自定义 SPI 服务加载器（支持标准与自定义路径、去重、线程安全缓存与异常隔离） |
| `com.peach.common.unique` | 统一 ID 生成 SPI 体系（UUID 去横杠、NanoID、雪花算法） |
| `com.peach.common.key` | 公共 key 定义契约、元数据维护接口和无业务语义的 key 构建器 |
| `com.peach.common.util.desensitize` | 手机号、邮箱、证件、IP、endpoint、错误消息等脱敏 |
| `com.peach.common.util.encrypt` | 对称加解密 SPI、密钥解析、密文编解码 |
| `com.peach.common.util` | 字符串、集合、时间、ID、随机数等基础工具 |
| `com.peach.common.constant` | 跨模块共享的基础常量 |


## 公共 key 维护

`peach-common` 负责维护跨模块可复用的 key 定义契约和构建能力，不保存验证码、登录、订单、缓存场景等具体业务 key。

- `KeyTemplate`：最小 key 模板契约，只声明 `pattern()`。
- `KeyDefinition`：带维护元数据的 key 定义契约，声明 `moduleCode()`、`keyIntroduce()`、`valueIntroduce()` 和 `author()`。
- `KeyBuilder`：根据原始 key、字符串模板或 `KeyTemplate` 构建最终 key。

```java
public enum CommonKey implements KeyDefinition {
    GLOBAL_LOCK("PEACH:COMMON:LOCK:{0}", "COMMON", "全局锁 key", "锁持有者", "Peach");

    private final String pattern;
    private final String moduleCode;
    private final String keyIntroduce;
    private final String valueIntroduce;
    private final String author;

    CommonKey(String pattern, String moduleCode, String keyIntroduce, String valueIntroduce, String author) {
        this.pattern = pattern;
        this.moduleCode = moduleCode;
        this.keyIntroduce = keyIntroduce;
        this.valueIntroduce = valueIntroduce;
        this.author = author;
    }

    @Override
    public String pattern() {
        return pattern;
    }

    @Override
    public String moduleCode() {
        return moduleCode;
    }

    @Override
    public String keyIntroduce() {
        return keyIntroduce;
    }

    @Override
    public String valueIntroduce() {
        return valueIntroduce;
    }

    @Override
    public String author() {
        return author;
    }
}

String lockKey = KeyBuilder.from(CommonKey.GLOBAL_LOCK, "job-1").getRealKey();
```

具体业务或组件 key 定义必须放在使用它的模块内。例如 CAPTCHA 相关 Redis key 位于 `peach-captcha-autoconfigure` 的 `com.peach.captcha.key.CaptchaRedisKey`，只复用 `peach-common` 的 `KeyDefinition` 和 `KeyBuilder`。

## 审计上下文

`PeachDO.fillCreateTime()` 和 `PeachDO.fillModifyTime()` 通过 `AuditContext` 读取当前操作人、租户和组织信息。`peach-common` 只定义 `AuditContextProvider` 接口，不直接依赖 Sa-Token 或其他安全实现。

默认 Sa-Token 桥接由 `peach-satoken-autoconfigure` 提供。业务方也可以声明自己的 `AuditContextProvider` Bean，自动配置会把最终 Bean 注册到 `AuditContext`。

```java
@Bean
AuditContextProvider auditContextProvider() {
    return new AuditContextProvider() {
        @Override
        public String currentUserId() {
            return "system";
        }

        @Override
        public String currentTenantId() {
            return "default-tenant";
        }

        @Override
        public String currentOrgId() {
            return "default-org";
        }
    };
}
```

请求态、线程态和登录态由 provider 所属模块负责维护，`AuditContext` 不保存请求对象或用户对象。

## 加解密

加解密入口为 `EncryptFactory.getEncrypt(type)`，当前 SPI 注册了：

- `EncryptConst.AES`：AES/GCM/NoPadding，新密文格式为 `v1:AES-GCM:<hex>`。
- `EncryptConst.SM4`：SM4/GCM/NoPadding，新密文格式为 `v1:SM4-GCM:<hex>`。
- `EncryptConst.DES`：兼容历史 DES/CBC 场景，仅用于旧数据迁移和兼容读取。

AES 和 SM4 新密文会为每次加密生成随机 IV，并在密文 payload 中携带 IV。无 `v1:` 前缀的历史 CBC 十六进制密文仍可解密，用于兼容已有数据。

密钥解析顺序为：JVM 系统属性 → 操作系统环境变量 → classpath `default/*.key` 文件。`default` 目录下的 key 只作为兜底策略，避免未配置时基础能力直接不可用；生产环境应通过系统属性或环境变量显式覆盖。

| 算法 | JVM 系统属性 | 环境变量 | 长度 |
| --- | --- | --- | --- |
| AES | `peach.common.encrypt.aes.key` | `PEACH_COMMON_ENCRYPT_AES_KEY` | 16、24 或 32 字节 |
| DES | `peach.common.encrypt.des.key` | `PEACH_COMMON_ENCRYPT_DES_KEY` | 至少 8 字节 |
| SM4 | `peach.common.encrypt.sm4.key` | `PEACH_COMMON_ENCRYPT_SM4_KEY` | 16 字节 |

密钥值可以使用明文 UTF-8 字节，也可以使用 `base64:` 前缀传入 Base64 编码后的密钥材料。default key 会随 classpath 打包，不能作为生产环境的安全隔离手段。

## 脱敏与错误消息清理

`DesensitizeUtil` 默认输出掩码结果，适用于日志、错误消息和非授权展示场景。对异常消息建议使用：

```java
String safeMessage = DesensitizeUtil.sanitizeErrorMessage(exception.getMessage(), 1000);
```

该方法会清理换行，掩盖常见 `password`、`token`、`secret`、`accessKey`、`credential` 和 `Bearer` 内容，并按长度截断。

## ID 生成器

全局统一 ID 生成入口为 `UniqueIdFacade`，底层通过 Java SPI（`ServiceLoader`）加载 `UniqueGeneratorProvider` 实现，支持开箱即用的内置算法与零侵入的自定义算法扩展。

### 1. 内置算法对比

| 算法标识 | 格式与特征 | 典型长度 | 有序性 | 适用场景 |
| --- | --- | --- | --- | --- |
| `IdGeneratorConst.UUID` (`uuid`) | 32 位十六进制小写字符串（去除横杠 `-`） | 32 字符 | 无序 | 默认算法；完全兼容历史既有业务主键逻辑 |
| `IdGeneratorConst.NANOID` (`nanoid`) | 采用 64 个安全字符（`0-9a-zA-Z_-`） | 21 字符 | 无序 | 紧凑型业务唯一标识、短链、分片临时 Session 等 |
| `IdGeneratorConst.SNOWFLAKE` (`snowflake`) | 64 位长整数或纯数字字符串 | 19 位数字 | 趋势自增 | 高并发分布式主键、日志 traceId、对 B+Tree 索引友好的数据表 |

### 2. 配置参数与多级兜底策略

无需修改代码，可通过 JVM 系统属性或操作系统环境变量灵活切换全局默认生成策略，并配置分布式雪花算法参数。

| 配置功能 | JVM 启动参数 | 操作系统环境变量 | 兜底优先级与回退规则 | 默认兜底值 |
| --- | --- | --- | --- | --- |
| **全局默认算法** | `peach.common.id.type` | `PEACH_COMMON_ID_TYPE` | 1. 系统属性<br>2. 环境变量<br>3. 若配置的算法未注册，打印告警日志并自动回退至默认值<br>4. 若默认值不可用，回退至首个已加载算法 | `uuid` |
| **雪花算法机器 ID** (worker-id) | `peach.common.id.snowflake.worker-id` | `PEACH_COMMON_ID_SNOWFLAKE_WORKER_ID`<br>（或简写 `PEACH_ID_WORKER_ID`） | 1. 系统属性 (0~31，非法值直接拒绝)<br>2. 环境变量 (0~31，非法值直接拒绝)<br>3. 本机网卡 MAC 低字节 / IP 尾段自适应哈希推导<br>4. 最终兜底默认值 | `1` |
| **雪花算法数据中心 ID** (datacenter-id) | `peach.common.id.snowflake.datacenter-id` | `PEACH_COMMON_ID_SNOWFLAKE_DATACENTER_ID`<br>（或简写 `PEACH_ID_DATACENTER_ID`） | 1. 系统属性 (0~31，非法值直接拒绝)<br>2. 环境变量 (0~31，非法值直接拒绝)<br>3. 本机 IPv4 倒数第二段特征哈希推导<br>4. 最终兜底默认值 | `1` |

### 3. 代码使用示例

```java
// 1. 获取全局默认算法生成的 ID（未配置时默认输出 32 位去横杠 UUID）
String id = UniqueIdFacade.nextId();

// 2. 指定算法类型生成 ID
String uuid = UniqueIdFacade.nextId(IdGeneratorConst.UUID);
String nanoId = UniqueIdFacade.nextId(IdGeneratorConst.NANOID);
String snowflakeStr = UniqueIdFacade.nextId(IdGeneratorConst.SNOWFLAKE);

// 3. 快捷辅助方法
String quickNano = UniqueIdFacade.generateNanoId();
String quickSnow = UniqueIdFacade.generateSnowflakeId();
long numericSnowflakeId = UniqueIdFacade.nextLongId(); // 获取 64 位 long 型雪花 ID

// 4. 存量兼容方法（100% 保持历史签名兼容）
String legacyUuid = IDGeneratorUtil.generateUuid();
```

`IDGeneratorUtil` 保留为历史兼容入口，新代码优先使用 `UniqueIdFacade`。

### 4. 自定义算法扩展（SPI）

如需接入自定义生成器（如美团 Leaf、百度 UidGenerator、特定业务前缀编码器等），无需改动 `peach-common`：

1. 实现 `UniqueGenerator` 接口并定义算法标识（如 `type() { return "leaf"; }`）。
2. 实现 `UniqueGeneratorProvider` 接口返回生成器实例。
3. 在扩展模块的 `src/main/resources/META-INF/services/com.peach.common.unique.IdGeneratorProvider` 中写入实现类的全限定名。
4. 业务中即可直接通过 `UniqueIdFacade.nextId("leaf")` 调用，或配置 `-Dpeach.common.id.type=leaf` 作为全局默认算法。

## 自定义服务加载器（CustomServiceLoader）

`CustomServiceLoader` 是对 JDK 标准 `ServiceLoader` 的扩展与增强，提供统一的 SPI 服务发现与装配能力。

### 核心特性
- **标准兼容**：优先无缝加载标准 `META-INF/services/` 下的 SPI 配置文件。
- **自定义路径支持**：支持传入自定义类路径（目录前缀如 `"META-INF/peach-services/"` 或具体文件路径），自动匹配并安全读取配置。
- **同类去重（Deduplication）**：标准路径与自定义路径若存在重复实现类，保持先发现者优先，单个实现类仅实例化一次。
- **线程安全缓存**：以 `(serviceClass, classLoader, customPaths)` 为键缓存加载实例列表，避免频繁扫描 ClassLoader 与重复反射。
- **异常隔离与容错**：单个配置行语法错误、类不存在或实例化异常均被局部隔离并记录告警日志，不阻塞其他可用实现的加载；支持行内 `#` 注释。
- **便捷操作**：支持 `load(Class<T>)`、`findFirst(Class<T>)`、`loadFirst(Class<T>, T default)`、`reload(Class<T>)` 及 `loadFromFile(Class<T>, File)`。

### 使用示例

```java
// 1. 标准加载所有可用实现（自动缓存）
List<IdGeneratorProvider> providers = CustomServiceLoader.load(IdGeneratorProvider.class);

// 2. 加载首个可用实现（若无实现返回默认值）
EncryptProvider provider = CustomServiceLoader.loadFirst(EncryptProvider.class, defaultProvider);

// 3. 支持自定义扩展目录
List<StorageProviderFactory> customList = CustomServiceLoader.load(
        StorageProviderFactory.class, "META-INF/peach-storage/");

// 4. 强制刷新缓存并重新加载
List<IdGeneratorProvider> reloaded = CustomServiceLoader.reload(IdGeneratorProvider.class);
```

## 边界约束

- 不在 `peach-common` 放具体业务缓存 key、Topic、表字段、领域状态码或业务流程。
- 不在 `peach-common` 放 Web 全局异常处理器、过滤器、拦截器或启动类。
- 不通过反射、静态查 Bean 或服务定位器绕过模块依赖边界。
- 不把生产密钥、token、私钥、签名 URL 或生产地址写入源码、README、测试快照或配置示例。
- 新增公共 API 前需要确认已有多个模块的稳定真实复用，并检查下游影响。

## 构建与验证

常用验证命令：

```bash
node scripts/check-utf8.mjs
mvn -pl peach-common -am test -Pdevelopment
git diff --check
```

涉及审计上下文或 Sa-Token 桥接时，至少额外运行：

```bash
mvn -pl peach-middleware/peach-satoken/peach-satoken-autoconfigure -am test -Pdevelopment
```

如果当前 shell 默认不是 JDK 21，需要先设置 `JAVA_HOME` 指向 JDK 21。

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 加密时报 `encryption key is not configured` | 对应系统属性、环境变量和 classpath default key 是否缺失或为空 | 补齐显式密钥；兜底 key 仅保证基础能力可用 |
| 解密历史数据失败 | 密文是否为旧 CBC 十六进制格式，密钥是否与历史数据一致 | 保持旧密钥可用，迁移后逐步重写为 `v1:` 新密文 |
| 审计字段为空 | 是否存在 `AuditContextProvider` Bean，是否已由 autoconfigure 注册 | 检查 Sa-Token starter 或自定义 provider 的装配 |
| 租户或组织上下文缺失 | 实体是否包含 `tenantId` / `orgId` 可写属性，provider 是否返回空 | 在服务层确认当前请求已完成租户/组织校验 |
| ID 生成器报 `No ID generator provider found` | 传入的算法类型是否拼写正确，对应 provider 是否已在 SPI 文件中注册 | 确认算法名称或通过 `IdGeneratorRegistry.register()` 手动注册 |
| 雪花算法报 `Clock moved backwards` | 系统时钟发生较大幅度（>5ms）回拨或跨时区突变 | 检查服务器 NTP 状态；避免在运行中向后突变服务器时间 |
| 下游模块编译失败 | 是否修改了公共类签名、常量名、异常构造器或工具方法返回语义 | 使用 `rg` 和 CodeGraph 检查调用方，保持兼容或同步迁移 |
