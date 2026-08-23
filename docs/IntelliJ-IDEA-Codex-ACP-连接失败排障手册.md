# IntelliJ IDEA 通过 ACP 连接 Codex 失败排障手册

> 适用范围：Windows 上使用 JetBrains AI Assistant 的 IntelliJ IDEA，通过 `codex-acp`（Agent Client Protocol, ACP）连接 Codex 时，出现“无法启动 ACP 进程”“无法解析 Agent 启动配置”或连接失败。
>
> 本文基于 2026-08-23 的实际排障记录编写。文中的版本号、目录名和代理端口均为当时环境的证据或示例；排障时必须以本机日志和实际 IDEA 版本为准。

## 1. 先分清 ACP 与 MCP

这两个连接方向不同，不能互相替代：

| 场景 | 协议与方向 | 本文是否处理 |
| --- | --- | --- |
| IDEA 聊天窗口选择 Codex Agent | IDEA -> `codex-acp` -> Codex | 是 |
| Codex 调用 IDEA 的代码浏览、编辑或构建能力 | Codex -> IDEA 内置 MCP SSE 服务 | 否 |

因此，项目 `.codex/config.toml` 中类似 `mcp_servers.idea` 的配置、IDEA MCP 端口返回 HTTP 200，均不能证明 ACP Agent 能够启动。ACP 故障应优先查看 IDEA 的 ACP 日志。

## 2. 本次故障的最终根因

本次失败链路由两个相互叠加的问题构成：

1. IDEA 通过自己的 Node 运行时执行 `npm install @agentclientprotocol/codex-acp@<固定版本>`。该运行时显式使用自己的 `NPM_CONFIG_USERCONFIG`，**不会读取**用户目录的 `%USERPROFILE%\.npmrc`。
2. IDEA 内置 npm 因未获得本机代理设置而直连 npm registry，日志出现 `ECONNRESET`，适配器安装失败。
3. 在手动验证 ACP 时启动的 `codex-acp` 子进程没有完全退出，锁住了适配器目录内的 `codex.exe`。之后 IDEA 清理旧安装目录时出现 `AccessDeniedException`，即使网络恢复也无法重装。

解决的关键不是反复在适配器目录手工安装，而是：

1. 给 **IDEA 实际读取的 npmrc** 配置正确网络策略；
2. 结束仅属于该缓存目录的遗留 ACP 进程；
3. 删除并重建该固定版本的适配器缓存；
4. 以 IDEA 相同的环境变量验证安装成功。

## 3. 目录、日志与变量

以下用 PowerShell 变量避免硬编码 IDEA 小版本。先选择当前使用的 IDEA 配置目录；例如本次为 `IntelliJIdea2026.2`。

```powershell
$ideaVersion = 'IntelliJIdea2026.2'
$ideaLocal = Join-Path $env:LOCALAPPDATA "JetBrains\\$ideaVersion"
$acpRoot = Join-Path $ideaLocal 'acp-agents'
$runtime = Join-Path $acpRoot '.runtimes\\node\\24.13.0'
$acpLog = Join-Path $ideaLocal 'log\\acp\\acp.log'
```

常用位置：

| 用途 | 位置 |
| --- | --- |
| ACP 主日志 | `%LOCALAPPDATA%\JetBrains\<IDE 版本>\log\acp\acp.log` |
| IDEA 总日志 | `%LOCALAPPDATA%\JetBrains\<IDE 版本>\log\idea.log` |
| ACP 适配器缓存 | `%LOCALAPPDATA%\JetBrains\<IDE 版本>\acp-agents\codex-acp\<版本>` |
| IDEA 内置 npm 用户配置 | `%LOCALAPPDATA%\JetBrains\<IDE 版本>\acp-agents\.runtimes\node\<版本>\npmrc` |
| IDEA 内置 npm 缓存与调试日志 | 同目录下的 `npm-cache\_logs` |

不要把 `auth.json`、API Key、ChatGPT 登录令牌、npm 私有 registry token 或代理认证信息复制到日志、项目文档或命令输出中。

## 4. 快速诊断流程

### 4.1 获取最新错误

先在 IDEA 中新建一个 Codex ACP 对话并发送一条短消息，再读取日志末尾。旧会话可能已经被标记为不可恢复，不能作为修复结果的验证入口。

```powershell
Get-Content -Tail 300 $acpLog
```

按错误关键字分类：

| 日志特征 | 含义 | 优先动作 |
| --- | --- | --- |
| `npm install ... failed`、`ECONNRESET` | 适配器下载链路失败 | 按第 5 节检查 IDEA 内置 npm 网络配置 |
| `Failed to prepare ... for npm install`、`AccessDeniedException` | 缓存目录被进程锁定 | 按第 6 节定位并结束精确进程树 |
| `Installed package ...` 后仍失败 | 安装已完成，继续查进程启动、认证或后端网络 | 查看 `AcpServerProcessHandlerImpl` 之后的 stderr 和 `idea.log` |
| `401`、`authentication`、登录窗口提示 | ACP 已启动但身份未建立 | 在 IDEA 内选择 ChatGPT 或 API Key 登录；不要手工编辑凭据文件 |
| `TLS`、`certificate`、`websocket`、`api.openai.com` | Codex 后端网络/证书问题 | 检查企业代理、VPN、根证书及网络策略；不要把它误判为 npm 安装问题 |

### 4.2 从 npm 调试日志确认真正使用的配置

`acp.log` 会给出 npm 调试日志路径。打开该文件，重点检查前 40 行：

```powershell
Get-Content -Head 40 '<npm 调试日志的完整路径>'
```

必须确认三点：

1. `verbose argv` 确实在安装 `@agentclientprotocol/codex-acp@<固定版本>`；
2. `config load:file:` 列出的 `npmrc` 中，哪个被 IDEA 加载；
3. `fetch manifest`、`GET` 或 `ECONNRESET` 对应的是哪个 registry。

不要仅根据命令行中普通用户的 `npm config list` 判断，因为 IDEA 通常会通过 `NPM_CONFIG_USERCONFIG` 覆盖用户 npm 配置。

## 5. 修复 npm 网络配置

### 5.1 检查 IDEA 实际的 npmrc

IDEA 进程日志中会出现类似：

```text
Set runtime env: NPM_CONFIG_USERCONFIG=...\\acp-agents\\.runtimes\\node\\<版本>\\npmrc
```

该路径才是需要修改的配置层。检查文件与连通性：

```powershell
$npm = Join-Path $runtime 'npm.cmd'
$env:NPM_CONFIG_USERCONFIG = Join-Path $runtime 'npmrc'
$env:NPM_CONFIG_CACHE = Join-Path $runtime 'npm-cache'

Get-Content -Raw $env:NPM_CONFIG_USERCONFIG
& $npm ping --registry=https://registry.npmjs.org
```

若环境需要本地 HTTP 代理，且已确认代理服务正在本机监听，可写入该 npmrc。以下 `127.0.0.1:7897` 只是本次环境的示例，必须改成当前环境实际可用的代理地址。

```powershell
& $npm config set proxy 'http://127.0.0.1:7897' --location=user
& $npm config set https-proxy 'http://127.0.0.1:7897' --location=user
& $npm config set fetch-retries 4 --location=user
& $npm config set prefer-offline true --location=user

Get-Content -Raw $env:NPM_CONFIG_USERCONFIG
& $npm ping --registry=https://registry.npmjs.org
```

说明：此处的 `--location=user` 指向的是通过 `NPM_CONFIG_USERCONFIG` 指定的 IDEA 内置 `npmrc`，不会修改 `%USERPROFILE%\.npmrc`。

若网络应当直连，不要写入失效代理；应先删除这两个键，再重新执行 `npm ping`：

```powershell
& $npm config delete proxy --location=user
& $npm config delete https-proxy --location=user
```

修改前后都必须执行 `npm ping`。只有 `PONG` 不能完全证明依赖包可下载，仍需执行第 7 节的实际安装验证。

## 6. 处理 `AccessDeniedException` 文件锁

当日志指向以下或相似文件时，说明缓存目录被占用：

```text
...\\codex-acp\\<版本>\\node_modules\\@openai\\codex-win32-x64\\...\\codex.exe
```

### 6.1 精确定位，不要按进程名批量结束

以管理员 PowerShell 查找命令行或可执行路径属于 **当前 IDEA 的 `codex-acp` 缓存目录** 的进程：

```powershell
Get-CimInstance Win32_Process |
  Where-Object {
    $_.ExecutablePath -like "*$ideaVersion\\acp-agents\\codex-acp*" -or
    $_.CommandLine -like "*$ideaVersion\\acp-agents\\codex-acp*"
  } |
  Select-Object ProcessId, ParentProcessId, Name, ExecutablePath, CommandLine |
  Format-List
```

必须核对完整路径。不要结束：

- `%APPDATA%\npm` 下用户主动运行的全局 Codex；
- 其他 IDE（例如 PyCharm、GoLand）自己的 ACP 缓存进程；
- 与本次目录无关的 `node.exe`、`codex.exe` 或 `java.exe`。

### 6.2 结束确认后的单一进程树

仅在路径和父子关系均确认后执行，`<根 PID>` 是上一命令中该树最上层的 `cmd.exe`、`node.exe` 或 PowerShell 进程：

```powershell
taskkill /PID <根 PID> /T /F
```

再次执行第 6.1 节命令，应不再返回当前 IDEA `codex-acp` 缓存目录的实际子进程。仅用于检查的 PowerShell 本身可能因命令行中包含路径而被匹配，需根据其命令内容排除。

## 7. 清理与重新安装（可验证修复）

先完全退出 IDEA，或确认第 6 节已释放锁，再清理**唯一确认的**适配器版本目录。不要删除整个 `%LOCALAPPDATA%\JetBrains`，也不要删除用户的 `.codex` 目录。

```powershell
$adapterVersion = '1.6.2' # 以 installed.json 或 ACP 日志中的实际版本为准
$adapterDir = Join-Path $acpRoot "codex-acp\\$adapterVersion"

Remove-Item -LiteralPath $adapterDir -Recurse -Force
```

使用与 IDEA 相同的环境变量重装并验证。固定版本来自 ACP registry/日志，禁止改成 `@latest`：

```powershell
$env:NPM_CONFIG_USERCONFIG = Join-Path $runtime 'npmrc'
$env:NPM_CONFIG_CACHE = Join-Path $runtime 'npm-cache'

& $npm install "@agentclientprotocol/codex-acp@$adapterVersion" `
  --prefix $adapterDir --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw 'Codex ACP adapter installation failed.' }

& $npm ls '@agentclientprotocol/codex-acp' --prefix $adapterDir --depth=0
Test-Path (Join-Path $adapterDir 'node_modules\\.bin\\codex-acp.cmd')
Test-Path (Join-Path $adapterDir 'node_modules\\@openai\\codex-win32-x64\\vendor\\x86_64-pc-windows-msvc\\bin\\codex.exe')
```

成功标准：npm 退出码为 0，`npm ls` 显示目标版本，两个 `Test-Path` 均为 `True`。本次已在 IDEA 实际的 `NPM_CONFIG_USERCONFIG` 和 npm 缓存环境下达到该标准；npm 日志中所有包下载均返回 HTTP 200。

## 8. IDEA 内最终验证

1. 重启 IDEA，避免旧的不可恢复 ACP 会话和旧文件句柄继续存在。
2. 新建一个 Codex ACP 对话，不要复用此前报错的会话。
3. 发送一个无副作用的短问题，例如“仅回复 `ACP connected`”。
4. 在 `acp.log` 中确认依次出现：
   - `Installed package '...codex-acp...'`（首次或缓存清理后）；
   - `Starting new process for agent 'Codex'`；
   - `Initializing ACP client session`；
   - 没有新的 `ConfigResolutionFailed`、`ECONNRESET` 或 `AccessDeniedException`。
5. 若显示登录卡片，按 IDEA 界面完成 ChatGPT 或 API Key 登录。登录成功后再次新建会话测试。

不要为了验证而从缓存目录手动长期运行 `codex-acp.cmd`。该命令会启动 `codex.exe app-server`；若进程未退出，会再次锁住适配器目录，导致 IDEA 后续清理和重装失败。

## 9. 回滚与持续维护

### 回滚网络配置

如果本机代理地址变更、代理服务关闭或切换为直连，删除 IDEA 内置 npmrc 中相应键：

```powershell
$env:NPM_CONFIG_USERCONFIG = Join-Path $runtime 'npmrc'
& $npm config delete proxy --location=user
& $npm config delete https-proxy --location=user
& $npm config delete fetch-retries --location=user
& $npm config delete prefer-offline --location=user
```

之后重新执行 `npm ping` 和第 7 节安装验证。不要把代理密码、token 或带认证信息的 URL 提交到本仓库。

### 版本更新后

JetBrains 更新 IDEA、其内置 Node 运行时或 ACP registry 中的 `codex-acp` 版本时，目录会变化。每次升级后：

1. 重新从 `acp.log` 确认实际的 `NPM_CONFIG_USERCONFIG`、Node 版本和适配器版本；
2. 检查新运行时目录下的 `npmrc` 是否仍含必要网络配置；
3. 以第 7 节的方式做一次固定版本安装验证；
4. 不要从旧 IDEA 目录复制凭据文件。

## 10. 证据与边界

本次排障使用的证据是 IDEA `acp.log`、其 npm 调试日志、受控进程的完整可执行路径，以及在 IDEA 实际 npm 环境下完成的固定版本安装。OpenAI 官方文档可用于 Codex 产品和 CLI 的一般指导，但本次 ACP 安装器的具体目录、环境变量与日志语义以本机 JetBrains 日志为准。

本文不保证代理、registry、JetBrains AI Assistant 或 ACP 包的未来行为不变；遇到新错误时，应先回到第 4 节按最新日志重新分类，而不是直接套用某个旧目录或 PID。
