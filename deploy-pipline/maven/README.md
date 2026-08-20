# Maven Settings 模板

本目录只保留一个模板：`settings.xml`。

目标策略：

- 本地开发优先从 Nexus `maven-public` 拉依赖。
- Nexus 不可达时，第三方依赖继续从阿里云 `public` 仓库解析。
- 私有 `com.peach` 构件、Release 发布、Snapshot 发布仍然依赖 Nexus。
- Jenkins 构建时自动基于 `settings.xml` 生成真实 settings，不再维护第二份 CI settings。
- Nexus 账号密码、代理账号密码都通过环境变量注入；未配置时自动省略对应 XML 块。

## 文件职责

| 文件 | 用途 |
| --- | --- |
| `settings.xml` | 唯一 Maven settings 模板；本地可复制，CI 自动渲染 |

生成文件不提交：

| 场景 | 生成位置 |
| --- | --- |
| 本地 | `~/.m2/settings.xml` |
| Jenkins | `/var/jenkins_home/.m2/settings.generated.xml` |

## 仓库顺序

| 用途 | id | URL |
| --- | --- | --- |
| 优先下载依赖、插件、私有构件 | `nexus-public` | `@MAVEN_NEXUS_URL@/repository/maven-public/` |
| Nexus 不可达时的第三方依赖回退 | `aliyun-public` | `@MAVEN_ALIYUN_PUBLIC_URL@` |
| 发布 Release | `peach-releases` | `@MAVEN_NEXUS_URL@/repository/maven-releases/` |
| 发布 Snapshot | `peach-snapshots` | `@MAVEN_NEXUS_URL@/repository/maven-snapshots/` |

这里不使用 `mirrorOf=*`。原因是 `mirrorOf=*` 会把所有仓库强制镜像到 Nexus，一旦 Nexus 不可达，Maven 不会继续尝试阿里云仓库。

## Jenkins 自动填充变量

`Jenkinsfile` 在 Maven 打包前调用 `deploy-pipline/scripts/render-maven-settings.mjs` 读取 `settings.xml`，先填充必要变量，生成 `/var/jenkins_home/.m2/settings.generated.xml`，然后通过 Jenkins 持久卷挂载给 Maven 容器执行：

```bash
mvn -s /var/jenkins_home/.m2/settings.generated.xml ...
```

可配置变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MAVEN_SETTINGS_TEMPLATE` | `$WORKSPACE/deploy-pipline/maven/settings.xml` | settings 模板路径 |
| `MAVEN_LOCAL_REPOSITORY` | `/var/jenkins_home/.m2/repository` | Jenkins 持久化 Maven 本地仓库 |
| `MAVEN_NEXUS_URL` | `${PEACH_NEXUS_URL:-http://nexus:8081}` | Jenkins 容器访问 Nexus 的基础地址 |
| `PEACH_NEXUS_URL` | `http://nexus:8081` | 兼容旧变量；流水线也用它覆盖 `pom` 中的 `peach.nexus.url` |
| `MAVEN_ALIYUN_PUBLIC_URL` | `https://maven.aliyun.com/repository/public` | Nexus 不可达时的第三方依赖回退仓库 |
| `MAVEN_NEXUS_USERNAME` | `peach-deploy.env.example` 中示例为 `Development` | Nexus 账号；账号和密码都存在时生成 `<servers>` |
| `MAVEN_NEXUS_PASSWORD` | `change_me_nexus_password` | Nexus 密码；上传 Jenkins 前必须替换，账号或密码为空时不生成 `<servers>` |
| `MAVEN_PROXY_HOST` | 空 | Maven 出网代理 host；为空时不生成 `<proxies>` |
| `MAVEN_PROXY_PORT` | 空 | Maven 出网代理 port；为空时不生成 `<proxies>` |
| `MAVEN_PROXY_PROTOCOL` | `http` | 代理协议 |
| `MAVEN_PROXY_ID` | `corp-proxy` | 代理 id |
| `MAVEN_PROXY_ACTIVE` | `true` | 代理是否启用 |
| `MAVEN_PROXY_USERNAME` | 空 | 代理账号；为空时不生成代理认证字段 |
| `MAVEN_PROXY_PASSWORD` | 空 | 代理密码；为空时不生成代理认证字段 |
| `MAVEN_PROXY_NON_PROXY_HOSTS` | `localhost\|127.0.0.1\|nexus\|*.aliyun.com\|maven.aliyun.com\|10.*\|172.*\|192.168.*` | 不走代理的主机；在 `.env` 中必须整体加引号 |

认证生成规则：

- `MAVEN_NEXUS_USERNAME` 和 `MAVEN_NEXUS_PASSWORD` 都存在：生成 `nexus-public`、`peach-releases`、`peach-snapshots` 三个 `<server>`。
- 任一为空：不生成 `<servers>`，适用于 Nexus 开启 Anonymous Read 的开发环境。
- `MAVEN_PROXY_HOST` 和 `MAVEN_PROXY_PORT` 都存在：生成 `<proxies>`。
- 代理账号密码都存在：生成 `<username>` 和 `<password>`。
- 代理账号密码任一为空：只生成无认证代理。

Jenkins Secret file 直接上传真实 `.env` 即可，不需要上传真实 settings。流水线会在 Maven 容器启动前填充 `settings.xml` 模板，并把生成后的 settings 放入 Jenkins 持久卷供 Maven 容器读取。`.env` 会被 Jenkins shell source；包含 `|`、空格、`#`、`&` 等特殊字符的值要加引号，代理排除列表推荐写成：

```dotenv
MAVEN_PROXY_NON_PROXY_HOSTS="localhost|127.0.0.1|nexus|*.aliyun.com|maven.aliyun.com|10.*|172.*|192.168.*"
```

`peach-deploy.env.example` 里的 `MAVEN_NEXUS_PASSWORD=change_me_nexus_password` 只是占位值。上传 Jenkins 前必须替换；如果 Nexus 开启 Anonymous Read，也可以同时删除或注释 `MAVEN_NEXUS_USERNAME` 和 `MAVEN_NEXUS_PASSWORD`。

## 本地使用

Windows PowerShell：

```powershell
New-Item -ItemType Directory -Force $env:USERPROFILE\.m2
Copy-Item deploy-pipline\maven\settings.xml $env:USERPROFILE\.m2\settings.xml
notepad $env:USERPROFILE\.m2\settings.xml
```

本地替换建议：

| 占位符 | 本地建议值 |
| --- | --- |
| `@MAVEN_LOCAL_REPOSITORY@` | `D:/Environment/repository` 或你的本地仓库路径 |
| `@MAVEN_NEXUS_URL@` | `http://nexus.peachsoft.com:8081` |
| `@MAVEN_ALIYUN_PUBLIC_URL@` | `https://maven.aliyun.com/repository/public` |
| `@MAVEN_NEXUS_USERNAME@` / `@MAVEN_NEXUS_PASSWORD@` | Nexus 关闭匿名读时填写 |
| `@MAVEN_PROXY_*@` | 需要公司代理时填写 |

本地手动编辑时：

- 不需要 Nexus 账号：删除 `@optional NEXUS_AUTH` 到 `@optional-end` 整段注释。
- 不需要代理：删除 `@optional MAVEN_PROXY` 到 `@optional-end` 整段注释。
- 需要代理但不需要代理认证：删除代理块里的 `@MAVEN_PROXY_AUTH@` 占位行。

不要把本地真实 `settings.xml`、账号、密码、代理密钥提交到 Git。

## Nexus 初始化

1. 启动 Nexus：

   ```bash
   docker compose -f deploy-pipline/pipline/docker-compose.yml up -d nexus
   ```

2. 获取初始密码：

   ```bash
   docker exec nexus cat /nexus-data/admin.password
   ```

3. 创建仓库：

   | 仓库 | 类型 | 说明 |
   | --- | --- | --- |
   | `aliyun-public` | proxy | Remote storage 指向 `https://maven.aliyun.com/repository/public` |
   | `maven-releases` | hosted | 发布 Release |
   | `maven-snapshots` | hosted | 发布 Snapshot |
   | `maven-public` | group | 成员包含 `aliyun-public`、`maven-releases`、`maven-snapshots` |

4. 开发环境可开启 Anonymous Read；关闭匿名读时，在 Jenkins Secret file 中配置 `MAVEN_NEXUS_USERNAME` 和 `MAVEN_NEXUS_PASSWORD`。

## 验证

本地：

```bash
mvn -U dependency:get -Dartifact=org.springframework.boot:spring-boot-starter:3.5.4
mvn -DskipTests validate -pl peach-common
```

Jenkins：

- `Build CI image` 阶段会先构建 Maven + Node CI 镜像。
- `Maven package` 阶段会先生成 settings，再执行 Maven。
- 日志中应能看到 `nexus:8081/repository/maven-public`；Nexus 不可达时，第三方依赖会继续尝试 `maven.aliyun.com/repository/public`。

## 排障

| 现象 | 处理 |
| --- | --- |
| Maven 401 | Nexus 关闭匿名读但未配置 `MAVEN_NEXUS_USERNAME` / `MAVEN_NEXUS_PASSWORD`，仍使用 `change_me_nexus_password` 占位值，或用户无对应仓库权限 |
| Nexus 不可达后第三方依赖没有回退阿里云 | 检查 settings 是否仍有 `mirrorOf=*`；当前模板不应出现该配置 |
| 私有 `com.peach` 构件拉不到 | 私有构件只在 Nexus；需要先启动 Nexus 或先发布对应 SNAPSHOT |
| 代理不生效 | 同时配置 `MAVEN_PROXY_HOST` 和 `MAVEN_PROXY_PORT`，确认 `MAVEN_PROXY_NON_PROXY_HOSTS` 已加引号且没有误排除目标域名 |
| 代理 407 | 同时配置 `MAVEN_PROXY_USERNAME` 和 `MAVEN_PROXY_PASSWORD` |
| Jenkins 依赖异常 | 清理 `/var/jenkins_home/.m2/repository` 后重跑流水线 |
| 中文日志乱码 | 本地确认 `.mvn/jvm.config` 生效；CI 重新执行 `Build CI image`，保留 `LANG=C.UTF-8`、`LC_ALL=C.UTF-8`、`MAVEN_OPTS` 和 `JAVA_TOOL_OPTIONS` 中的 UTF-8、`user.language=en`、`user.country=US` |
