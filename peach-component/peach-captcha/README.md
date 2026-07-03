# peach-captcha

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-captcha`  
类型：验证码组件聚合模块

## 模块定位

`peach-captcha` 提供验证码生成、缓存、校验、频控和扩展点。业务模块通过 `peach-captcha-starter` 接入，避免在业务代码中重复实现验证码缓存和校验流程。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-captcha-autoconfigure` | 核心 API、配置绑定、自动配置、默认实现 |
| `peach-captcha-starter` | 对业务模块暴露的 starter |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `CaptchaProperties` | 绑定验证码配置，配置前缀由 `CaptchaConst.CAPTCHA_SUFFIX` 声明 |
| `CaptchaService` | 验证码生成和校验服务 |
| `CaptchaCacheService` | 验证码缓存服务 |
| `CaptchaServiceProvider` | 验证码服务 provider |
| `CaptchaCacheProvider` | 缓存 provider |
| `FrequencyLimitHandler` | 频控扩展点 |

## 接入方式

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-captcha-starter</artifactId>
</dependency>
```

业务侧优先注入 `CaptchaService` 使用验证码能力。需要替换缓存、生成器或频控策略时，通过自定义 Bean 或 provider 覆盖默认实现。

## 运行机制

1. starter 引入 autoconfigure。
2. 自动配置读取 `CaptchaProperties`。
3. 根据配置装配验证码生成服务和缓存服务。
4. 生成验证码时写入缓存，校验时读取并比对。
5. 频控处理器可限制同一用户、IP 或业务 key 的调用频率。

## 边界与限制

- 验证码不能替代登录风控、账号锁定和设备识别。
- 缓存实现决定验证码是否支持多实例共享；生产环境不应依赖单机内存缓存。
- 校验成功后是否删除验证码、失败次数如何限制，需要结合当前实现和业务策略确认。
- 不应在日志中输出验证码明文。

## 构建与验证

```bash
mvn -f "peach-component/peach-captcha/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-component/peach-captcha -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| `CaptchaService` 未注入 | 是否引入 `peach-captcha-starter`；自动配置条件是否满足 | 检查依赖树和 Spring 条件报告 |
| 多实例校验失败 | 缓存是否跨实例共享 | 使用 Redis 等共享缓存实现 |
| 验证码频繁失效 | 过期时间、缓存 key、系统时间是否正确 | 检查配置和缓存记录 |
| 频控不生效 | `FrequencyLimitHandler` 是否注册 | 检查自定义 Bean 和默认实现 |
