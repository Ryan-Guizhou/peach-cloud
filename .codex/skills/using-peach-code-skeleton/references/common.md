# Common Placement

目标：

- 把复用代码放到正确层级，减少业务模块横向污染，同时贴合当前仓库真实落点。

当前仓库的常见落点：

- 模块内通用常量：常见放在 `xxx-common` 或模块下 `common` 包，很多采用 `interface XxxConst` / `interface XxxConstant`。
- 模块内工具类：常见放在 `auth.common`、`message.common` 之类包下，例如 `RsaPasswordUtil`。
- 全局基础能力：放在 `peach-common`，例如 `PeachDO`、`PeachEntity`、`Response`、`CurrentContext`、通用校验/工具类。

归属规则：

- 只在当前模块内复用的方法、常量、枚举、转换器、工具类，优先放当前模块 `common`。
- 跨多个业务模块稳定复用，且不携带具体业务语义的能力，才进入 `peach-common`。
- 带领域语义、表语义、流程语义的常量，不要直接扔进 `peach-common`。
- 不要因为“以后可能复用”就提前上提到 `peach-common`。

结合现有仓库的具体判断：

- 缓存 key、模块编码、业务状态码、WebSocket topic、文件域状态这类内容，优先留在模块 `common`，参考 `MessageConst`、`SettingConst`、`FileDomainConstant`。
- 当前仓库常见 `interface Const` 风格，新增模块常量时优先沿用，不要突然切成另一套 `final class + private constructor`，除非该模块已经统一这么写。
- 模块包名中如果已有存量拼写，例如 `peach.setting.comon.enums` 的 `comon`，新代码优先复用现有包结构，不要在同一模块里再平行造一套 `common/enums` 导致分裂。

上提到 `peach-common` 的条件：

- 与具体业务模块无关。
- 不依赖某个业务域的 DTO/DO/VO/QO。
- 提上去不会引入反向依赖、循环依赖或新的模块耦合。
- 真的已经被多个模块稳定使用，而不是预判将来会用。

不应上提的情况：

- 只服务于单个模块某个 service 的 helper。
- 只在一两个查询中使用的 SQL 常量或状态码。
- 依赖领域对象、业务状态流转、消息类别、文件状态等业务语义。

案例 1：应该留在模块 common

```java
/**
 * 消息模块常量。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime yyyy/M/d HH:mm
 */
public interface MessageConst {

    String MODULE_CODE = "MESSAGE";

    String WEBSOCKET_REDIS_TOPIC = "peach:message:websocket:push";
}
```

案例 2：应该进入 peach-common

```java
public final class IdCardMaskUtil {

    private IdCardMaskUtil() {
    }

    public static String mask(String value) {
        // 仅示意：不依赖任何业务模块对象
        return value;
    }
}
```

提交前检查：

- 这个常量/工具是模块内复用，还是全局基础能力。
- 是否已经依赖某个业务模块对象；如果依赖了，就不应进入 `peach-common`。
- 当前模块已有的常量组织方式是 `interface Const` 还是别的风格，新代码是否保持一致。
- 是否因为包名洁癖去“修正”现有存量包结构，导致同模块产生双轨 common。
