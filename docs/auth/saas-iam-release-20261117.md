# peach-auth SaaS 身份、租户与权限底座改造设计（release_20261117_v1.0.0）

> 状态：**实施设计 / 尚未实现**。基线：2026-11-17 从 \`main\` 分支创建的 \`release_20261117_v1.0.0\`。开发流程：**一个中文 Issue → 一个 PR → 验证 → 合并 release**；不直接变更 \`main\`。
>
> 范围：\`peach-auth\` 为主要业务域，必要同步 \`peach-code\`、\`peach-common\`、\`peach-satoken\`、\`peach-gateway\`、\`peach-auth-openfeign-external\`、\`peach-cloud-front\` 以及实际受影响的微服务。该文档只陈述已核对的 Current 和待实施 Proposed，不能以本文代替代码验证。

## 1. 决策冻结（Implementation Contract）

| 主题 | 统一结论 |
| --- | --- |
| 租户隔离 | Shared DB / Shared Table；所有租户级读写、缓存、令牌、异步与 Feign 调用必须强制租户隔离 |
| 用户身份 | \`PEACH_USER\` 全局身份；\`PEACH_TENANT_MEMBER\` 是用户在一个租户的独立成员身份 |
| 组织与授权 | \`PEACH_USER_ORG\` 表示人员组织关系；\`PEACH_AUTH_PARTY\` 表示成员角色和机构范围，不混用 |
| 角色 | 归属租户；一成员可拥有多个角色，同一角色可在多个独立组织范围分别绑定 |
| FISCAL | 本轮移除年度权限维度，包含 SQL、Java、Feign、缓存、前端 |
| 所有物理主键 | 所有实体和关系表统一 \`ID\`，**单列主键**；默认使用 \`UniqueIdFacade.nextId()\` 生成 32 字符无连字符 UUID，允许由受控配置切换到现有支持的算法 |
| 主键切换安全 | UUID、NanoID、Snowflake 等切换不改变表/DTO字段名，必须校验最大长度、大小写敏感、碰撞空间、旧记录兼容、写入后读出、并发；不将可配置默认生成器误认为永远返回 UUID |
| 永久业务 ID | \`USER_ID\` / \`TENANT_ID\` / \`ORG_ID\` / \`ROLE_ID\` 等：由 peach-code 产生，**跨租户全局永久唯一、永不重用**；存储 UNIQUE 不含删除标志 |
| 业务 CODE | \`ORG_CODE\` / \`ROLE_CODE\` 等；规则生成或受控人工定义，是否可重用按领域决定；\`project:read\` 等权限码是稳定契约，不能被随机重新发号 |
| 物理关联 | \`USER_REF_ID\` / \`TENANT_REF_ID\` / \`ORG_REF_ID\` / \`ROLE_REF_ID\` / \`MEMBER_REF_ID\` 指向目标表物理 \`ID\`；禁止通过可能被重用的 CODE 关联 |
| 删除 | 所有业务删除为逻辑删除并保留历史；物理清除涉及法律合规的个人数据时使用单独受控合规流程，不等同普通业务删除 |
| 有效记录唯一 | 使用 \`ACTIVE_SLOT = CASE WHEN IS_DELETE=0 THEN 1 ELSE NULL END\` 生成列和唯一索引；不能简单 \`UNIQUE(key, IS_DELETE)\` |
| 审计 | 创删改、授权/撤销、恢复及敏感操作记录操作者、上下文、时间和变更原因，不记密码、Token、密钥、完整敏感报文 |
| 项目规范 | \`AGENTS.md\` + \`.agents/skills/using-peach-java-engineering\` + \`.agents/skills/using-peach-front\`；Java 21、构造器注入、Lombok、中文 Javadoc（类型保留既有作者/版本/时间）、英文日志、不写全限定类引用、不留无用 import、SQL/XML 与实体类型一致 |
| 文档 | \`README.md\` 中文与 \`README.en-US.md\` 英文同步；本方案保存在 \`docs/auth/\`，每期变更修正实际实现状态 |
| PR 规则 | 一个 Issue 对应一个 PR；目标必须是 \`release_20261117_v1.0.0\`；每 PR 只覆盖该 Issue 必需的影响面；本轮不直接合入 main |

### 1.1 ID 命名与关联示例

\`\`\`text
PEACH_USER
  ID           = 550e8400e29b41d4a716446655440000   # 物理 ID / 主键
  USER_ID      = USER_00000001                   # 全局永久业务 ID
  USER_CODE    = zhangsan                        # 登录标识，不等于 USER_ID

PEACH_ORGANIZATION
  ID           = 6da8b67a03e04b7e97f64749d0830001
  ORG_ID       = ORG_00000001                    # 全局永久业务 ID
  ORG_CODE     = EAST-RD                         # 可按机构编码规则维护
  TENANT_REF_ID= <PEACH_TENANT.ID>

PEACH_TENANT_MEMBER
  ID           = 7a3c22f49ac34a7e856b1443c0f30002
  MEMBER_ID    = MEMBER_00000001                 # 若有成员业务编号需求
  USER_REF_ID  = <PEACH_USER.ID>
  TENANT_REF_ID= <PEACH_TENANT.ID>

PEACH_AUTH_PARTY
  ID            = <独立物理ID>
  MEMBER_REF_ID = <PEACH_TENANT_MEMBER.ID>
  ROLE_REF_ID   = <PEACH_ROLE.ID>
  SCOPE_TYPE    = ORG_TREE
  SCOPE_REF_ID  = <PEACH_ORGANIZATION.ID>
\`\`\`

实体表并非必须为每条授权明细另外分配业务编号。只有存在业务展示/外部对接场景，才为关系表增加 \`MEMBER_ID\`、\`GRANT_ID\` 等业务编号；物理主键 \`ID\` 必须始终存在。

**不允许混用：** \`xxxId\` 为永久业务 ID，\`xxxRefId\` 为物理外键，通用 CRUD 使用 \`id\`，服务端根据授权上下文限制可读 ID；修改、删除、查询均不得只按 ID 而省略租户和有效状态。

### 1.2 全局业务 ID 的唯一范围

先建立平台级序列命名空间，发出 \`USER_00000001\`、\`TENANT_000001\`、\`ORG_00000001\`、\`ROLE_00000001\` 等**全局**业务标识。所有类型必须有不会相互碰撞的固定类型前缀/全局命名空间。数据库对应业务 ID 建立全局 \`UNIQUE\`（如 \`UNIQUE(ORG_ID)\`），不以 \`TENANT_REF_ID + ORG_ID\` 替代；仍须在查询时同时验证 \`TENANT_REF_ID\`，因为 ID 不可猜测不等于授权。

如果业务 CODE 需要按租户生成，可以使用租户命名空间，如华星的 \`ORG_CODE=EAST-RD\` 和星河的 \`ORG_CODE=EAST-RD\` 可以并存。编码与物理 ID 互不转化。

## 2. 代码现状（Current，按 main 核对）

| 已核对文件 | 当前问题 | 必须变更 |
| --- | --- | --- |
| \`sql/PEACH_AUTH_PARTY.sql\` | 物理主键是多列联合索引，虽然表里有 \`ID\` | 改为 \`ID\` 单列 PK，其他关系使用业务唯一约束 |
| \`sql/PEACH_CODE_RULE.sql\` | \`TENANT_ID+ORG_ID+CODE_PREFIX\` 联合 PK；实际发号索引是租户+前缀 | 改 \`ID\` PK，平台/租户命名空间与水位分离 |
| \`sql/PEACH_USER.sql\` | \`USER_ID\` 既是物理主键又是账号领域字段；\`PASSWORD VARCHAR(32)\` | \`ID\` PK、\`USER_ID\` 全球永久业务 ID、密码散列存储 |
| \`sql/PEACH_ROLE.sql\` | \`ROLE_ID\` 主键，\`ORG_ID\` 与 \`FISCAL\` 参加角色定义 | 角色归租户、\`ID\` PK、业务 \`ROLE_ID\` 永久唯一、去 Fiscal |
| \`sql/PEACH_ORGANIZATION.sql\` | 只有扁平机构信息，无父机构；\`ORG_ID\` 当前 PK | 新增 \`PARENT_ORG_REF_ID\`，\`ID\` PK，逻辑删除唯一约束 |
| \`sql/PEACH_USER_ORG.sql\` | 唯一 \`(TENANT_ID,USER_ID,ORG_ID)\`，\`USER_ID\` 使用旧主键语义 | 改为 member/tenant/org 的物理外键，迁移人事关系 |
| \`UserServiceImpl.java\`, \`UserDao.xml\` | 前端 RSA 加密后服务端解密，Base64 密码参与 SQL 条件比较；登录强制存在机构 | 服务端密码哈希验证、无机构成员也可进入租户 |
| \`LoginDTO\`, \`SwitchContextDTO\`, \`RoleDao.xml\` | 权限依赖 \`FISCAL\`、机构和角色编码 | 完整移除年度，换成员+角色+作用域 |
| \`LoginPermissionAssembler\` | \`INIT_APP_ID\` 写死，授权快照扁平化 | 按 appRefId、成员角色绑定与版本评估 |
| \`LoginPermissionCacheRefresher\` | 部分缓存 Key 只含 userId，还有 legacy 双写 | 改租户/成员/应用/版本组合，撤销立即失效 |
| \`GatewayTokenContextResolver\` | 要求 orgId 非空，仍带 fiscal | tenant/member 必需，org 可选，去年度 |
| \`GatewayAuthorizationGlobalFilter\` | 部分缺少 API 权限依赖时会放行 | 受保护资源默认拒绝，业务服务二次校验 |
| \`peach-cloud-front/src/types/auth.ts\` | \`fiscal\` 在登录信息、会话、请求等结构中 | 去年度并增加 member/tenant/current-app 结构 |
| \`peach-cloud-front/src/api/admin.ts\` | \`partyCode\`、\`userCode\`、\`orgId\` 和 \`fiscal\` 参与授权接口 | 换成员、角色和作用域的 UUID 关联契约 |
| \`peach-cloud-front/src/layouts/admin/index.vue\` | 租户/机构组成一个选择器；退出主要清本地状态 | 分离租户切换与工作机构切换；后端注销 |
| \`peach-cloud-front/src/router/dynamic.ts\` | 授权为空存在本地管理菜单回退 | 仅用后端授权的菜单/路由定义业务导航 |
| \`peach-cloud-front/src/views/system/organization\` / \`role\` | 通用表单直接编辑租户和领域 ID | 专用表单/组织树；只读生成业务号；物理 id CRUD |

本表不是代码实现声明。迁移后必须做跨仓库引用分析：peach-setting、peach-fileservice、peach-message、peach-gateway、peach-satoken、各启动服务和全量 SQL。

## 3. 身份/授权实体关系（目标）

\`\`\`text
PEACH_USER (平台全局用户，ID PK，USER_ID 业务ID)
     └── PEACH_TENANT_MEMBER (ID PK，USER_REF_ID，TENANT_REF_ID)
             ├── PEACH_USER_ORG (关联实际就职机构：ORG_REF_ID)
             └── PEACH_AUTH_PARTY (MEMBER_REF_ID, ROLE_REF_ID, SCOPE_TYPE, SCOPE_REF_ID)
                     └── PEACH_ROLE (ID PK，ROLE_ID 业务ID，TENANT_REF_ID)
                              ├── PEACH_ROLE_PERMISSION (ROLE_REF_ID, RESOURCE_REF_ID)
                              ├── PEACH_DATA_POLICY（角色/资源/作用域数据策略）
                              └── PEACH_FIELD_POLICY（角色/资源/字段读写策略）

PEACH_TENANT (ID PK，TENANT_ID 业务ID)
     ├── PEACH_ORGANIZATION (ID PK，ORG_ID 业务ID，PARENT_ORG_REF_ID 树结构)
     ├── PEACH_ROLE
     ├── PEACH_RESOURCE / PEACH_APPLICATION / PEACH_MENU / PEACH_ROUTER
     └── PEACH_TENANT_MEMBER

PEACH_RESOURCE_FIELD (字段注册，RESOURCE_REF_ID)
PEACH_AUTH_LOG (变更不可篡改审计语义)
PEACH_CODE_RULE (ID PK，配置命名空间/水位/格式)
\`\`\`

### 3.1 角色与数据权限

- 角色 \`PEACH_ROLE\` 由租户定义，不再把“角色归属机构”与“角色在机构内生效”混为同一字段。
- \`PEACH_AUTH_PARTY\` 可以为一个成员的同一角色建立多个独立的 \`ORG_TREE\` / \`ORG\` / \`TENANT\` 授权范围。
- \`PEACH_USER_ORG\` 为人事归属，不自动拥有机构管理员角色。
- 先针对**每条有效授权绑定**计算“该操作是否允许、该对象是否在绑定范围内、数据策略是否允许”；只有整条绑定有效时才将结果合并。禁止把角色的操作权限、机构范围分开求并集。
- 行级数据策略：\`SELF / ORG / ORG_TREE / TENANT_ALL\`，实际结果必须落在当前租户内、角色绑定允许范围内。
- 字段策略：\`ALLOW / MASK / DENY\` 读取，\`ALLOW / DENY\` 写入；对多重绑定定义确定性的冲突规则，敏感字段默认拒绝，后端实体到 VO 必须经过白名单映射，前端显示与导出使用同一评估结果。
- 对操作删除/批量修改/异步导出/文件下载，业务服务必须二次检查数据权限，网关 API 校验不取代业务数据层授权。

## 4. 统一的数据库约束与逻辑删除

### 4.1 标准字段

所有实体、授权和可变关系表采用：\`ID\`（物理主键）、\`IS_DELETE\`（0/1）、\`STATUS\`（若需要业务生命周期）、\`CREATED_AT/UPDATED_AT\`（时间列）、\`CREATOR_REF_ID/UPDATER_REF_ID\`（操作者物理ID）、\`DELETED_AT\`、\`DELETER_REF_ID\`、\`VERSION\`（乐观锁）。

现有项目使用 \`CREATED_TIME/MODIFY_TIME\` 字符串和 \`PeachDO\` 通用审计字段；迁移必须同批调整 DO、XML、审计填充，不得只改 SQL 使生产启动失败。是否新改为 DATETIME 统一由 Issue #202 实施并验证。

### 4.2 永久唯一与有效唯一严格区分

**永久业务 ID 不受删除影响：**

\`\`\`sql
PRIMARY KEY (ID),
UNIQUE KEY UK_ORG_ID (ORG_ID)
\`\`\`

业务 ID 被逻辑删除之后仍保留这个唯一索引和历史水位，禁止重新分发。全局用户登录名/身份凭据通常也不能无核验地复用。

**允许重用的业务 CODE：**

\`\`\`sql
IS_DELETE TINYINT(1) NOT NULL DEFAULT 0,
ACTIVE_SLOT TINYINT GENERATED ALWAYS AS (
  CASE WHEN IS_DELETE = 0 THEN 1 ELSE NULL END
) STORED,
UNIQUE KEY UK_ORG_ACTIVE_CODE (TENANT_REF_ID, ORG_CODE, ACTIVE_SLOT)
\`\`\`

MySQL 唯一索引允许多个 NULL，所以可以存在多个历史重复 CODE；只有一个有效 CODE 能存在。不要使用 \`UNIQUE(TENANT_REF_ID, ORG_CODE, IS_DELETE)\`，第二次逻辑删除会冲突。模板示例与实际 DDL 必须基于确定的 MySQL 版本测试。

**成员/授权关系**：例如 \`PEACH_TENANT_MEMBER\` 建议 \`UNIQUE(TENANT_REF_ID, USER_REF_ID)\` 永久唯一，成员离开后恢复原记录，但历史角色绑定仍不能自动恢复。组织成员关系、角色授权优先用逻辑撤销与审计事件区分历史；涉及重复授权时，使用活动唯一键+数据库并发裁决。

**删除恢复**：先验证同租户、目标父级、业务关联和有效唯一；冲突返回业务错误，不自动覆盖新记录；被撤销的授权绝不因为恢复角色/成员而重新生效。

**删除级联**：不得 CASCADE 物理删除人事关系、角色、资源、审计。机构有有效子机构、成员、授权或业务对象引用时，禁止直接删除，改走“停用/移交/撤销再删除”流程。

### 4.3 主键算法切换风险

\`UniqueIdFacade.nextId()\` 由配置选择默认生成器。当前支持 UUID、NanoID、Snowflake，要求：
1. 表列与接口采用可容纳所有批准算法的字符串长度；不得因旧 \`CHAR(32)\` 假定新 ID 永远 32 位。
2. 字符串比较必须大小写敏感；NanoID 包含大小写，默认不安全的大小写不敏感排序规则不能用于 ID 唯一键。DDL 应明确对 ID/FK 的二进制等价比较策略，方案不添加表级字符集和排序规则声明。
3. 主键切换是平台运维变更，生产前跑迁移/跨算法兼容性检查和多节点并发验证，禁止未经审批随意切换。
4. 物理 ID 绝不能含租户可推断的授权逻辑；是否可访问始终以服务端 tenant/member/role 评估。

## 5. peach-code：两类业务编码的发号方案

### 5.1 Current

\`CodeGenerator.next(tenantId,prefix)\` 使用 MySQL \`REQUIRES_NEW\` 事务、\`SELECT ... FOR UPDATE\` 锁定 \`TENANT_ID+CODE_PREFIX\` 的规则；产生 \`PREFIX_00000001\`。它不具备平台级全局业务类型命名空间、自由模板和多类型语义编码系统。不可将 “发号失败时回退 Redis/内存” 作为替代；独立事务允许出现断号。

### 5.2 Proposed

- \`PEACH_CODE_RULE.ID\` 采用统一物理主键，保留可安全扩展的命名空间：\`SCOPE_TYPE = PLATFORM / TENANT\`、\`SCOPE_REF_ID\`、\`CODE_TYPE\`、\`PREFIX\`、\`SEPARATOR\`、\`WIDTH\`、\`CURRENT_VALUE\`、\`STATUS\`、\`VERSION\`。
- \`USER_ID/TENANT_ID/ORG_ID/ROLE_ID\` 的永久业务 ID 在 **PLATFORM** 全局命名空间发号，并在业务表上创建 **全局** \`UNIQUE(XXX_ID)\`。不同租户不能重发 \`ORG_000001\`。
- \`ORG_CODE/ROLE_CODE\` 可以选择全局或租户级规则，若编码允许删除后重用，应有活动唯一索引；\`TENANT_OWNER\`、\`project:read\` 等固定安全语义编码由系统定义，不得依赖租户可编辑发号规则。
- 规则修改必须检查老前缀/新前缀碰撞，历史序号不能回退；模板版本化记录；代码生成与业务插入使用业务幂等键协调，发生断号可接受。
- Java API 可沿用现有 \`CodeGenerator\` 公开接口并增加受控 \`CodeType\` / \`CodeScope\` 调用接口，不随意给现有方法加入破坏性必填参数；尽量保持独立 Starter 的接口简洁。
- 必须提供 MySQL 多连接竞争测试，重复/断电/异常/规则改动测试，以及 Starter \`additional-spring-configuration-metadata.json\` 与中英文文档同步。

## 6. 登录、租户选择与授权刷新

\`\`\`text
浏览器登录
 -> /auth/init（公钥/验证码）
 -> /auth/login（账号+经 RSA 保护的密码载荷）
 -> 服务端解密并验证强密码哈希、账号状态与失败策略
 -> 加载属于该用户的有效 PEACH_TENANT_MEMBER 列表
 -> 只有一个有效租户：显式绑定当前 tenant/member 会话
 -> 多个有效租户：返回待选租户状态，不先赋予任一租户权限
 -> /auth/selectTenant（核验成员身份，变更可信上下文）
 -> 生成 server-side Sa-Token 上下文 + authVersion + 权限摘要
 -> API Gateway 校验登录/粗粒度资源
 -> 业务 Service 校验操作 + 行 + 字段
 -> /auth/switchTenant 或 /auth/logout：刷新/吊销旧上下文与缓存
\`\`\`

登录态至少包含 \`USER_REF_ID\`、\`TENANT_REF_ID\`、\`MEMBER_REF_ID\`、\`APP_REF_ID\`、\`AUTH_VERSION\`。机构可空；人事归属/工作机构切换不能改变已经授予的角色生效边界。

**安全要求**：选择租户只能选有效成员，不能直接拿浏览器上传的 \`TENANT_REF_ID\` 当可信信息；成员撤销、租户停用、角色变更后不得继续沿用旧权限缓存；浏览器只负责展示授权列表，不是安全决策源。本人个人信息与租户级用户目录分开，\`UserVO\` 不应继续继承含密码的 \`UserDO\`。

## 7. 网关、Feign 与微服务信任边界

- Gateway 负责登录检查与稳定 API 资源权限；不存在匹配授权的 protected API 一律拒绝，Redis 权限组件缺失或连接失败不能退化成“放行”。
- 服务端执行租户隔离和具体业务对象权限；禁止仅在网关校验。对无登录公共端点单独允许名单配置及测试。
- \`peach-auth-openfeign-external\` 承担向其他微服务提供必要成员身份、权限验证和安全上下文能力；不得暴露含 password/hash 的数据库 DO。
- 服务间身份必须可信（网关过滤/签名或服务鉴权）；客户端能够伪造的 \`TENANT_REF_ID\`、\`USER_REF_ID\` 请求头不得直接成为授权依据。
- Redis 缓存键至少区分 \`TENANT_REF_ID + MEMBER_REF_ID + APP_REF_ID + AUTH_VERSION\`，对平台认证凭证与租户业务权限使用不同 key 空间。每次版本变化，旧缓存不可继续授权。
- 涉及 Nacos/OSS/Redis/文件/消息/调度的异步场景必须由任务消息携带服务端验证过的租户上下文；异步消费不得盲信不明来源租户头。

## 8. 前端契约（Vue3 + TS + Pinia + Router + Ant Design Vue）

### 8.1 明确 API 字段

| 语义 | TypeScript 字段 | 说明 |
| --- | --- | --- |
| 当前记录物理主键 | \`id\` | CRUD 主键，非业务号 |
| 用户永久业务 ID | \`userId\` | 只读，不能当物理外键 |
| 机构永久业务 ID | \`orgId\` | 只读，允许查询展示 |
| 角色永久业务 ID | \`roleId\` | 只读 |
| 机构业务 CODE | \`orgCode\` | 可规则生成/业务指定 |
| 物理关联 | \`memberRefId/roleRefId/orgRefId/tenantRefId\` | 当前绑定与关联 |
| 当前权限版本 | \`authVersion\` | 上下文切换/失效使用 |
| 原年度 | ~~\`fiscal\`~~ | 完全删除 |

建议 API 请求例如：

\`\`\`json
{
  "memberRefId": "<PEACH_TENANT_MEMBER.ID>",
  "roleRefId": "<PEACH_ROLE.ID>",
  "scopeType": "ORG_TREE",
  "scopeRefId": "<PEACH_ORGANIZATION.ID>"
}
\`\`\`

\`tenantRefId\` 默认由可信登录上下文决定而非允许前端任意指定；平台跨租户管理使用独立受控接口。

### 8.2 前端组件改造清单

- \`src/types/auth.ts\`：删除 fiscal，增加 \`TenantMembership\`、\`CurrentTenantContext\`、\`RoleBinding\`、\`PermissionSnapshot\`；避免 \`string\` 隐含多种 ID 语义。
- \`src/api/auth.ts\`：新增选择/切换租户；退出服务端调用；错误与登录未完成租户选择状态一致。
- \`src/api/admin.ts\`：停止提交 \`partyCode/userCode/orgId/fiscal\` 旧绑定组合；以物理外键+scope 操作。
- \`src/stores/auth.ts\`：按用户+租户成员+授权版本隔离当前权限；租户切换后清除菜单/路由和旧授权快照，重新拉取。
- \`src/layouts/admin/index.vue\`：租户切换与工作机构选择分别显示，允许没有机构的租户成员；退出统一调用后端再清本地。
- \`src/router/dynamic.ts\`：受保护管理菜单全部依赖服务端授权，去掉权限列表为空时的完整 fallback 管理导航；组件路径必须由前端本地白名单导入。
- \`src/views/system/organization\`：通用 CRUD 替换为组织树、父子/移动校验；\`id\` 为 row-key，\`orgId\` 只读，\`orgCode\` 受规则控制；不允许任意手输租户 ID。
- \`src/views/system/role\`、\`authorization\`：角色功能权限 + 成员多角色、多机构范围绑定；动态显示数据权限、字段权限；移除年度表单。
- 新增租户成员邀请/禁用/恢复与租户管理页；按实际能力分层提供权限策略界面，错误、loading、empty、disabled、键盘操作覆盖。

## 9. Java、Mapper、REST、Feign 编码规范与门禁

- Java 21，按现有 Maven 模块及包名组织；\`peach-auth-common/entity/service/rest/external/launch\` 职责不混淆，不新建权限微服务。
- DTO / DO / QO / VO 分离；禁止 \`UserVO extends UserDO\` 等可能序列化敏感信息的继承；\`BeanUtils.copyProperties\` 不用于混合敏感字段/隐式覆盖权限字段。
- Spring Bean 单一构造器注入（\`@RequiredArgsConstructor\`、\`private final\`）；\`@Transactional\` 放在有效代理的业务入口；DAO Mapper 使用精确租户/ID/删除状态条件，参数绑定防注入。
- **中文 Javadoc、英文日志**；类型注释保留 \`@Author\`、\`@Version\`、\`@CreateTime\`；清理类名和 import 中不必要的全限定类名，避免通配符与没用的 import；错误码/常量/枚举集中定义；Lombok 用于确有价值的数据模型。
- 不使用无效 SQL 硬删除：将 \`delById\` 业务语义映射为带租户/版本的 \`UPDATE IS_DELETE=1\`，必要时改成更明确的 \`softDelete\`；真实数据库历史与审计不丢失。
- Feign 请求/响应单独契约；服务启动依赖和 Nacos/Redis 缺失失败行为可预期；业务返回统一错误语义，不吞异常/敏感堆栈不外泄。
- \`PeachDO\` 审计时间字段若有变更必须保证整个受影响项目一致；MyBatis XML、DO 字段、SQL 顺序、默认值、索引、乐观锁版本一致。
- 保留当前独立 README 命名：中文 \`README.md\`，英文 \`README.en-US.md\`；共用属性需要 metadata 并执行仓库校验，不新增冗余 CI。

## 10. 迁移、兼容与回滚

1. 清点所有 \`USER_ID/TENANT_ID/ORG_ID/ROLE_ID\` 旧物理引用，包括 JWT/SaToken 会话、缓存、Feign、外部接口、现有业务 SQL、文件/消息/调度表。
2. 生成旧主键 → 新物理 ID → 新永久业务 ID 映射，验证每个调用方使用目标实体 ID 的语义；历史审计事件保留原始值与映射。
3. 本轮允许对未投产数据模型直接重建，但要保留必需的种子与可复现脚本；如仓库已存在真实数据，必须先备份、校验并由具体 Issue 选择可执行的分步迁移路径。
4. 停写迁移窗口下完成旧 ID 到新关联、唯一索引与外键校验；初始化全局 peach-code 历史水位，避免不同租户重复业务 ID。
5. 禁止旧权限事实源与新授权事实源无保护双写；权限缓存全部按新 Key 失效，原有 token 不能在新契约下误继承旧角色。
6. 对高风险代码（账号密码、租户隔离、角色撤销）不提供隐式旧式 fail-open 兼容；可以通过灰度与受控回滚到一致快照而不是同时混跑旧授权。
7. 生产 ID 算法变更需单独审批并完成预检查；不能仅改变 \`peach.common.id.type\` 就宣称可安全切换。

## 11. 验证与安全验收（所有适用项必须有实际证据）

| 编号 | 场景 | 通过标准 |
| --- | --- | --- |
| A01 | 默认发号 | \`ID\` 为 32 位无横杠 UUID；字段名均为 ID |
| A02 | 算法切换 | UUID/NanoID/Snowflake 试验模式无重复、无大小写冲突、无长度截断 |
| A03 | 业务 ID | 全局唯一、不因租户不同或逻辑删除复用 |
| A04 | 历史重复 CODE | 多个 deleted CODE 可共存，仅一条 active |
| A05 | 多次删除重建 | 同业务 CODE 创建/软删三次无索引冲突 |
| A06 | 恢复冲突 | 存在 active CODE 时历史恢复拒绝，不改变数据 |
| A07 | 逻辑删除 | 标准业务读写无 deleted 记录；DB 历史完整 |
| A08 | 密码验证 | 数据库存密码哈希，登录失败/锁定/重置符合预期 |
| A09 | 多租户账号 | 同一全局用户在多租户不同权限，切换不串权 |
| A10 | 无组织成员 | 租户成员有权限即可登录，不要求组织归属 |
| A11 | 角色多机构 | 同一角色在华东和华南分别有效，不含华北 |
| A12 | 角色交叉 | 不能把 A 角色动作和 B 角色的组织范围拼成越权 |
| A13 | 行级查询 | 详情、分页、导出、批量修改均受数据权限约束 |
| A14 | 字段读写 | 明文/脱敏/拒绝以及写入拒绝均由后端强制 |
| A15 | 账号禁用 | 用户或成员禁用后旧会话不可再授权 |
| A16 | 权限撤销 | 版本失效，旧 Redis Key/Token 不可继续放行 |
| A17 | 组织树 | 禁止环、跨租户父级和有效子树删除 |
| A18 | 网关故障 | Redis 缺失/超时时 protected API 拒绝，不 fail-open |
| A19 | Feign 调用 | 消费方编译启动通过，身份传递不可被浏览器头伪造 |
| A20 | 前端登录 | 租户选择、切换、退出登录、无机构成员路径可用 |
| A21 | 前端授权 | row-key=id；业务 ID 只读；角色和范围组合保存可用 |
| A22 | 工程门禁 | \`node scripts/verify-changes.mjs\` + 影响面 Maven/npm 构建通过 |
| A23 | 文档 | 中英文 README 同步，实际行为与设计状态一致 |
| A24 | 无遗留 | 不存在活跃旧 FISCAL/联合 PK/物理硬删除的认证域实现 |

## 12. 研发任务与严格依赖

> 所有任务为中文 Issue，一个 Issue 只能对应一个 PR，base 为 \`release_20261117_v1.0.0\`。若任务属于跨模块真实影响面，可在同一 PR 中修改受影响文件，不另开无意义的“同步修复 PR”。

| 阶段 / Issue | 核心交付 | 依赖 |
| --- | --- | --- |
| [#201](https://github.com/Ryan-Guizhou/peach-cloud/issues/201) | 方案冻结、文档与验收基线 | 无 |
| [#202](https://github.com/Ryan-Guizhou/peach-cloud/issues/202) | ID 命名 / PK / 逻辑删除 / DDL / Mapper 基础 | #201 |
| [#203](https://github.com/Ryan-Guizhou/peach-cloud/issues/203) | peach-code 全局永久业务 ID 与规则 | #202（接口契约可先协同设计） |
| [#204](https://github.com/Ryan-Guizhou/peach-cloud/issues/204) | 全局账号认证 / 安全密码 / 多租户会话 | #202, #203 |
| [#205](https://github.com/Ryan-Guizhou/peach-cloud/issues/205) | 租户成员 / 邀请 / 组织树 / 人事关系 | #202, #203 |
| [#206](https://github.com/Ryan-Guizhou/peach-cloud/issues/206) | RBAC / 资源目录 / 多机构绑定 | #204, #205 |
| [#207](https://github.com/Ryan-Guizhou/peach-cloud/issues/207) | 行级数据 / 字段权限 / 授权引擎 | #206 |
| [#208](https://github.com/Ryan-Guizhou/peach-cloud/issues/208) | Sa-Token / Gateway / Redis 鉴权一致性 | #204, #206 |
| [#209](https://github.com/Ryan-Guizhou/peach-cloud/issues/209) | Feign / 业务跨微服务上下文 | #204, #206 |
| [#210](https://github.com/Ryan-Guizhou/peach-cloud/issues/210) | Vue 登录、租户、组织、角色、数据字段权限界面 | #204, #205, #206, #207 |
| [#211](https://github.com/Ryan-Guizhou/peach-cloud/issues/211) | 最终联调、SQL 与文档同步、验收记录 | #202–#210 |

建议顺序：#201 → #202 → #203 → (#204、#205) → #206 → (#207、#208、#209) → #210 → #211。并行任务必须确保 PR 不相互依赖尚未合入 release 的接口。

## 13. 研发 PR 和 CI 统一门禁

- PR 标题和描述使用中文，写明 \`Closes #N\`（确保只关联自身 Issue）；只指向 release 分支。
- 先进行受影响模块 Maven / npm 最小充分编译、测试；若有测试失败先修复；需要跨端安全验证时加集成测试。
- 仓库唯一通用校验入口：\`node scripts/verify-changes.mjs\`（含 UTF-8、工程规范、Java Style、Spring Wiring、metadata、README 与 docs 检查等）；**不是每个 commit 都跑全量 CI**。
- PR CI 与必要测试全部通过后才合入 release；不能以耗时为由跳过关键安全门禁；最终 #211 对 release 做集中回归。
- SQL 不增加冗余 \`ENGINE\` / 表级 \`CHARACTER SET\` / \`COLLATE\` 定义，历史不合适定义直接清理但先验证实际依赖。
- 接口不能未经验证宣称已能运行；每个 PR 报告**实际文件变更、实际运行命令与结果、未验证项、剩余风险**。

## 14. 非目标与未决技术风险

- 本阶段不实现完整计费/订阅 SaaS 商业体系、LLM 功能或无关业务重构。
- 不新增独立 IAM 微服务；\`peach-auth\` 维持既有模块拆分。
- 不实现任意可执行的编码模板脚本、不允许权限码由用户随意生成。
- MySQL 生成列索引行为需在项目实际 MySQL 版本上测试；特别留意 ID 大小写敏感性。
- 代码改造前必须核对所有现有表物理字段映射；历史记录迁移和密码迁移不可仅靠示例 SQL 完成。
- 设计文档合入不等于研发完成；后续 Issues 未合并前均标记 Proposed。
