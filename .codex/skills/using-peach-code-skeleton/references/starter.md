# Starter Review And Implementation

用于 starter/autoconfigure/quickstart/SPI 的新增、修改或审查。先按 AGENTS.md 读取当前客户端的 10-architecture-and-starters；专项 Scheduler/Storage/Rocket/Email/Redis/Virtual Thread 规则仍按实际能力选择。

## Evidence

读取受影响 POM、自动配置注册文件、配置绑定类、配置元数据、公共接口、默认实现、quickstart 示例及相关测试；以这些文件建立“依赖 → 装配条件 → Bean → 资源/副作用”的关系。配置文件不是默认值证据，未声明的 Bean 覆盖行为不得推测。

## Work

1. 确认改动属于普通接入还是 starter 实现；业务接入使用已有入口，不直接修改底层 SDK 契约。
2. 列出本次变化涉及的开关、缺失依赖、错误配置、自定义 Bean、资源关闭场景，检查消费者兼容性。
3. 维护 `autoconfigure / starter / quickstart` 三层职责：autoconfigure 实现装配，starter 控制传递依赖，quickstart 展示可运行接入。
4. starter POM 只传递业务接入必需依赖；可选 SDK、provider、测试工具、示例依赖使用 optional、独立 provider 或 quickstart 隔离。
5. 配置属性必须能生成 IDE 提示；需要手工补充时维护 `additional-spring-configuration-metadata.json`。
6. 使用已有扩展点；只有真实隔离需求才新增模块或 SPI，不复制 Scheduler 的全部层级到简单工具。
7. 实现类不承担业务租户权限决策；来自业务的可信标识与策略必须有明确契约。
8. 公共配置/API/运行边界变化同步相关 README；内部等价调整不强制文档改写。

## Verification

装配、资源与契约变化按 10 规则执行相应测试；纯审查输出证据与缺口，不自动实施。未运行测试时只能报告静态检查，不能把 README 示例或注解存在写成验证通过。
