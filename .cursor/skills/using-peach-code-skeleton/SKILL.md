---
name: using-peach-code-skeleton
description: 约束 peach-cloud 的 REST、Entity、DAO/XML、Service、common 分层代码编写与审查；要求安全和正确性优先于存量风格，并执行 UTF-8 与构建门禁。
---

# Peach Code Skeleton

## Workflow

1. 判断任务层级：`rest`、`entity`、`dao.xml`、`service`、`common`，只读取对应 reference。
2. 扫描当前模块同类实现，区分目标风格与仅供兼容的历史模式。
3. 修改公共 API、DAO/Service 签名、XML `id`、公共模型前评估影响面。
4. 先检查敏感数据、事务、资源、权限和模块边界，再检查命名与排版。
5. 完成后运行 UTF-8 无 BOM检查、受影响模块编译/测试和差异检查。

## Precedence

用户最新要求 > 安全与正确性 > `AGENTS.md` > rules > 本 skill > 模块局部风格 > 历史存量代码。

`FORBIDDEN`：

- 为“拟合风格”复制完整 DTO 日志、敏感字段响应、错误日志级别、无效注解、事务自调用、资源泄漏或跨层依赖。
- 未确认外部库行为就补注解、配置、默认值或 SPI。
- 一次任务涉及一组同类文件时只修改其中一个。
- 使用系统默认编码读写中文文件，或产生 UTF-8 BOM。

## References

- REST：`references/rest.md`
- Entity / DO / DTO / QO / VO：`references/entity.md`
- DAO 与 MyBatis XML：`references/dao-xml.md`
- Service：`references/service.md`
- common / peach-common：`references/common.md`

涉及 README、RocketMQ、Storage、Threadpool 时叠加对应 skill；不加载无关 reference。

## Definition Of Done

- 分层、签名、校验、DAO/XML 和文档联动完整。
- 未记录或返回敏感字段，日志使用显式非敏感字段白名单。
- Java 21 和当前框架版本兼容。
- `node scripts/check-utf8.mjs` 通过。
- 受影响模块编译或测试通过；无法运行的检查在最终回复中明确说明。
- `git diff --check` 通过，且未覆盖用户无关改动。
