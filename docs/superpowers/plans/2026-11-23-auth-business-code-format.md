# Auth business code format implementation plan

> **Superseded — 2026-11-24：** 用户确认项目未上线，数据库改用首个完整 V1，在全新空 schema 中一次完成 schema、seed、真实编码与权限初始化。本文 V15 向前升级方案与以下执行记录仅为历史证据，不再是待执行步骤。旧 V14 开发库不能直接应用新 V1、clean/repair 或自动接管；使用新 schema，或由使用者明确备份后重建。四类编码格式、前端只读规则及资源权限标识不变的约定继续有效。

**Goal:** 实际迁移菜单/功能/路由/角色编码，并为新增记录自动分配 M/F/RT/RL 四位编码。

**Architecture:** 复用 CodeGenerator。向前 Java Flyway 迁移建立持久映射与账本，同步所有源码可证引用。前端新增显示自动生成、编辑编码只读，关联字段继续使用真实业务编码。

**Spec:** [实施约定](../specs/2026-11-23-auth-business-code-format.md)，用户明确要求继续完成。

## Constraints

资源权限标识、ID、URL 不变；四位耗尽拒绝；已执行 V12/V13/V14 不改；保留全部前序未提交修复；无远程提交。

## Tasks

- [x] Backend: Menu/Function/Role/Router 创建调用统一发号，编码不可改；菜单修复会话作用域、主键生成；充分单元测试与编译。
- [x] Migration: 新增 V15 Java 迁移、注册与映射表，先校验再迁移四类历史编码及所有引用；校准规则账本；failure fixture移 V16；真实隔离MySQL验证。
- [x] Frontend: 菜单/功能/角色/路由编辑自动发号显示，旧编码只读；验证功能树和角色授权不依赖旧编码字面量；测试、构建。
- [x] Integration: 独立审查、运行受影响测试、统一门禁，更新升级文档，说明实际业务库和会话生效边界。

## Review focus

跨租户/应用/机构/年度引用、角色同名跨上下文、已有符合格式编码/序列碰撞、异常引用导致授权丢失、主键和资源权限不变、前端仍提交可编辑编码。

## Ledger

- 用户针对截图再次明确要求调整现有记录；视为按已选前缀/位数/资源暂不改的实施授权，不重复确认。

- Historical verification: frontend 18 tests and build passed; three Chrome fixture regressions passed; auth unit tests 17 passed (2 optional DB smoke skipped); isolated MySQL migration suite 14 passed. The development database was observed at V14 during this work; the proposed V15 upgrade was superseded by the fresh-schema V1 decision above and is not pending execution.
