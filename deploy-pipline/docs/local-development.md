# 本地开发与 Maven 依赖

本文说明在 **Windows + Docker Desktop + Nexus 优先、阿里云回退** 配置下，如何在本地拉依赖、刷新缓存、构建与 IDE 联调。

## 前置条件

| 项 | 要求 |
| --- | --- |
| JDK | 21（与根 `pom.xml` 一致） |
| Maven | 3.9+ |
| Docker | DevOps 栈已启动（至少 Nexus） |
| hosts | 见下方列表 |

```text
127.0.0.1 nexus.peachsoft.com
127.0.0.1 peach_cloud.peachsoft.com
127.0.0.1 jenkins.peachsoft.com
127.0.0.1 gitlab.peachsoft.com
127.0.0.1 registry.peachsoft.com
127.0.0.1 nacos.peachsoft.com
```

## 一次性配置

### 1. Nexus

1. 启动：`docker compose -f deploy-pipline/pipline/docker-compose.yml up -d nexus`
2. 创建仓库（详见 [`../maven/README.md`](../maven/README.md)）：
   - `aliyun-public` proxy → `https://maven.aliyun.com/repository/public`
   - `maven-public` group → `aliyun-public` + `maven-releases` + `maven-snapshots`
3. 用户 `Development`（或自建）需有上述仓库 read；发布还需 releases/snapshots 写权限。

### 2. Maven settings

```powershell
New-Item -ItemType Directory -Force $env:USERPROFILE\.m2
Copy-Item deploy-pipline\maven\settings.xml $env:USERPROFILE\.m2\settings.xml
notepad $env:USERPROFILE\.m2\settings.xml
```

替换模板中的占位符。确认：

- `@MAVEN_LOCAL_REPOSITORY@` → `D:/Environment/repository`（或你的路径）
- `@MAVEN_NEXUS_URL@` → `http://nexus.peachsoft.com:8081`
- `@MAVEN_ALIYUN_PUBLIC_URL@` → `https://maven.aliyun.com/repository/public`
- `<server><id>nexus-public</id>` 与仓库 id 一致
- Nexus 开启 Anonymous Read 时，删除 `@optional NEXUS_AUTH` 到 `@optional-end` 整段注释。
- 不需要代理时，删除 `@optional MAVEN_PROXY` 到 `@optional-end` 整段注释。
- 需要代理但不需要代理认证时，删除代理块里的 `@MAVEN_PROXY_AUTH@` 占位行。

**不要**把含密码的 settings 提交到 Git。

### 3. 验证 Nexus 可达

```powershell
curl http://nexus.peachsoft.com:8081/service/rest/v1/status
curl http://localhost:8081/service/rest/v1/status
```

### 4. 验证 Maven

在仓库根目录：

```powershell
mvn -q dependency:get "-Dartifact=org.springframework.boot:spring-boot-starter:3.5.4"
mvn -q -DskipTests validate -pl peach-common
```

Nexus 正常时日志应优先出现 `nexus.peachsoft.com:8081/repository/maven-public`，且无 401。停止 Nexus 后，第三方依赖可继续从 `maven.aliyun.com/repository/public` 解析；私有 `com.peach` 构件仍需要 Nexus。

---

## 本地构建命令

仓库包含 `.mvn/jvm.config`，本地 Maven 会自动使用 UTF-8，并把 Maven/Javac 诊断语言固定为英文，避免 Windows 控制台或 Jenkins 日志中出现中文 warning 被错误解码后的乱码。

```powershell
# 全量打包（跳过测试）
mvn -DskipTests clean package

# 只构建某模块及其依赖
mvn -DskipTests clean package -pl peach-auth -am

# 发布 SNAPSHOT 到私服
mvn -DskipTests deploy -pl peach-common -am
```

pom 默认 `peach.nexus.url=http://nexus.peachsoft.com:8081`，与 settings 中 Nexus 地址保持一致即可。

---

## 依赖刷新（常见场景）

Maven 常见路径是：**本地 `.m2`** → **Nexus maven-public** → **Nexus 上游 aliyun-public**。如果 Nexus 不可达，settings 会继续尝试 **Aliyun public** 作为第三方依赖回退仓库。刷新前先判断卡在哪一层。

### 场景 A：改了 pom 依赖版本，本地仍用旧 jar

```powershell
# 强制检查 SNAPSHOT / 远程更新
mvn -U -DskipTests clean package

# 或只解析依赖
mvn -U dependency:resolve -pl peach-common -am
```

`-U` = `--update-snapshots`，对 SNAPSHOT 和 `updatePolicy=always` 的仓库生效。

### 场景 B：只刷新某个 com.peach 模块

```powershell
# 删除本地该构件（路径随 groupId/artifactId/version 变化）
Remove-Item -Recurse -Force D:\Environment\repository\com\peach\peach-common\1.0.0-SNAPSHOT -ErrorAction SilentlyContinue

mvn -U -pl peach-auth -am dependency:resolve
```

或：

```powershell
mvn dependency:purge-local-repository "-DmanualInclude=com.peach:peach-common" -DreResolve=false
mvn -U dependency:resolve -pl peach-auth -am
```

### 场景 C：第三方依赖损坏 / 下载不完整

```powershell
# 删除具体坐标目录，例如 Guava
Remove-Item -Recurse -Force D:\Environment\repository\com\google\guava\guava\33.4.8-jre -ErrorAction SilentlyContinue

mvn dependency:get "-Dartifact=com.google.guava:guava:33.4.8-jre"
```

### 场景 D：全量清空本地仓库（最后手段）

```powershell
# 仅删除 peach 或全部（会重新下载，耗时）
Remove-Item -Recurse -Force D:\Environment\repository\* 
mvn -DskipTests clean package
```

### 场景 E：Nexus 侧缓存了错误版本

1. 浏览器打开 `http://nexus.peachsoft.com` → `aliyun-public` → **Invalidate cache**
2. 或对 `maven-public` group 中对应 proxy 执行清理
3. 本地再执行 `mvn -U dependency:resolve`

### 场景 F：刚 deploy 了 SNAPSHOT，其他模块拉不到

```powershell
# 1. 确认私服已有构件：Nexus → maven-snapshots → Browse → com/peach/...
# 2. 消费方强制更新
mvn -U -pl peach-auth -am clean package
```

### 场景 G：Jenkins CI 依赖异常

```powershell
# 清理 Jenkins 持久化 .m2（会重新从 Nexus 拉）
docker compose -f deploy-pipline/pipline/docker-compose.yml exec jenkins rm -rf /var/jenkins_home/.m2/repository
```

然后在 Jenkins 重新 **Build Now**。

---

## IntelliJ IDEA

1. **Settings → Build → Build Tools → Maven**
   - Maven home：Bundled 3.9+ 或本机 Maven
   - User settings file：`C:\Users\<你>\.m2\settings.xml`
   - Local repository：`D:\Environment\repository`
2. 右键项目 → **Maven → Reload Project**
3. 勾选 **Always update snapshots**（可选，等同 `-U`）
4. **Invalidate Caches** 仅在前述刷新无效时使用

---

## 与 CI 的差异

| 项 | 本地 | Jenkins CI |
| --- | --- | --- |
| settings | `~/.m2/settings.xml` | `/var/jenkins_home/.m2/settings.generated.xml`，由 `settings.xml` 自动渲染 |
| Nexus URL | `http://nexus.peachsoft.com:8081` | `http://nexus:8081`（可用 `MAVEN_NEXUS_URL` 覆盖；兼容 `PEACH_NEXUS_URL`） |
| pom 属性 | 默认 `peach.nexus.url=...nexus.peachsoft.com` | 流水线追加 `-Dpeach.nexus.url=http://nexus:8081` |
| 本地仓库 | `D:/Environment/repository` | `/var/jenkins_home/.m2/repository`（持久卷） |
| Nexus 账号 | 手动写入本地 settings | Secret file 中设置 `MAVEN_NEXUS_USERNAME` / `MAVEN_NEXUS_PASSWORD` |
| Maven 代理 | 手动写入本地 settings | Secret file 中设置 `MAVEN_PROXY_HOST` / `MAVEN_PROXY_PORT`，需要认证时再加账号密码 |

CI 若关闭 Nexus 匿名读：只需要在 `peach-deploy.env` Secret file 中设置 `MAVEN_NEXUS_USERNAME` 和 `MAVEN_NEXUS_PASSWORD`。Jenkinsfile 会自动生成 `<servers>`，不需要再挂载单独的 settings 文件。

如果把 `peach-deploy.env.example` 复制后直接上传 Jenkins，必须先替换 `MAVEN_NEXUS_PASSWORD=change_me_nexus_password`。需要 Maven 出网代理时，`MAVEN_PROXY_NON_PROXY_HOSTS` 这类包含 `|` 的值要整体加引号，否则 Jenkins shell 加载 `.env` 时会把 `|` 当成管道。

---

## 排障速查

| 现象 | 处理 |
| --- | --- |
| 401 Unauthorized | 检查 settings 中 `nexus-public` / `peach-*` 的 `<server>` 与 Nexus 用户权限；匿名读开启时可删除 `<servers>` |
| Could not transfer com.peach artifact ... Connection refused | 私有构件只在 Nexus；启动 Nexus 或先发布对应 SNAPSHOT |
| 第三方依赖没有回退到阿里云 | settings 未生效；确认 `-s` 路径或 IDEA 指向正确 settings，且没有旧的 `mirrorOf=*` |
| 依赖版本不对 | `mvn -U` + 删除本地对应目录 |
| HTTP blocked | settings 缺少 `maven-default-http-blocker` 且 `blocked=false` |

更多流水线问题见 [`../README.md`](../README.md) 常见问题表。
