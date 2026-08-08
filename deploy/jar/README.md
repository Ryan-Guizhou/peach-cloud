# Peach Cloud java -jar 部署方案

本目录只负责 `java -jar` 方式的文件目录规划和业务服务启停脚本。它与 `deploy/peach.sh` 的 Docker Compose 方案隔离，不启动 MySQL、Redis、Nacos、Nginx，也不导入 SQL 或 Nacos 配置。

## 目录规划

```text
deploy/jar/
  peach-jar.sh          # java -jar 启停脚本
  .env.example          # 示例环境变量
  .env                  # 本机或服务器实际配置，不提交
  runtime/
    pids/               # 每个服务一个 pid 文件
    logs/
      peach-auth/
      peach-fileservice/
      peach-gateway/
      peach-generator/
      peach-message/
      peach-monitor/
      peach-setting/
    upload/
      peach-fileservice/
```

业务 jar 仍使用 Maven 模块自身的 `target/` 产物，不复制到 `deploy/jar/`：

| 服务 | jar 路径 | 默认端口 |
| --- | --- | --- |
| `peach-gateway` | `peach-gateway/peach-gateway-launch/target/peach-gateway-launch-1.0.0-SNAPSHOT.jar` | `18080` |
| `peach-auth` | `peach-auth/peach-auth-launch/target/peach-auth-launch-1.0.0-SNAPSHOT.jar` | `18081` |
| `peach-monitor` | `peach-monitor/peach-monitor-launch/target/peach-monitor-launch-1.0.0-SNAPSHOT.jar` | `18082` |
| `peach-fileservice` | `peach-fileservice/peach-fileservice-launch/target/peach-fileservice-launch-1.0.0-SNAPSHOT.jar` | `18083` |
| `peach-message` | `peach-message/peach-message-launch/target/peach-message-launch-1.0.0-SNAPSHOT.jar` | `18084` |
| `peach-setting` | `peach-setting/peach-setting-launch/target/peach-setting-launch-1.0.0-SNAPSHOT.jar` | `18085` |
| `peach-generator` | `peach-generator/peach-generator-launch/target/peach-generator-launch-1.0.0-SNAPSHOT.jar` | `18086` |

## 使用前提

1. 服务器已安装 JDK/JRE 8，并且 `java -version` 可用。
2. 已提前完成 Maven 打包：

```bash
./mvnw -DskipTests package
```

3. MySQL、Redis、Nacos 已由外部方式启动并初始化完成。
4. Nacos 中已经存在业务需要的配置。

## 初始化

```bash
cd deploy/jar
chmod +x peach-jar.sh
./peach-jar.sh init
```

初始化后编辑 `deploy/jar/.env`，重点确认：

```env
NACOS_SERVER_ADDR=127.0.0.1:8849
MYSQL_HOST=127.0.0.1:3307
REDIS_HOST=127.0.0.1:6380
MYSQL_ROOT_PASSWORD=change_me_mysql_root_password
REDIS_PASSWORD=change_me_redis_password
```

## 启停命令

| 命令 | 含义 |
| --- | --- |
| `./peach-jar.sh start` | 启动全部业务服务 |
| `./peach-jar.sh start peach-auth` | 只启动 `peach-auth` |
| `./peach-jar.sh stop` | 停止全部业务服务 |
| `./peach-jar.sh stop peach-auth` | 只停止 `peach-auth` |
| `./peach-jar.sh restart` | 重启全部业务服务 |
| `./peach-jar.sh restart peach-gateway` | 只重启 `peach-gateway` |
| `./peach-jar.sh status` | 查看全部服务进程状态 |
| `./peach-jar.sh status peach-message` | 查看单个服务进程状态 |
| `./peach-jar.sh logs peach-auth --tail 200` | 查看单个服务最近 200 行日志 |
| `./peach-jar.sh logs peach-auth --tail 200 -f` | 跟随查看单个服务日志 |
| `./peach-jar.sh logs all --tail 100` | 查看所有已存在日志文件最近 100 行 |

日志写入：

```text
deploy/jar/runtime/logs/<service>/nohup.log
```

PID 写入：

```text
deploy/jar/runtime/pids/<service>.pid
```

## 失败后怎么处理

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 提示 jar 不存在 | 对应模块 `target/` 下是否已有 jar | 回到仓库根目录执行 `./mvnw -DskipTests package` |
| 启动后马上退出 | `runtime/logs/<service>/nohup.log` | 优先检查 Nacos、MySQL、Redis 地址和密码 |
| 端口被占用 | `.env` 中对应 `*_PORT` | 修改端口后 `restart` 对应服务 |
| `status` 显示 stopped，但端口仍被占用 | 可能不是本脚本启动的进程 | 用系统命令确认进程来源，不要直接删除 `runtime/` |
| Nacos 配置读取不到 | Nacos namespace、group、dataId 是否正确 | 先修复 Nacos 配置，再 `restart <service>` |

这套脚本不管理中间件数据，因此一般不需要删除目录重来。除非你明确只想清理日志和 pid，可以删除 `deploy/jar/runtime/logs` 或 `deploy/jar/runtime/pids`；不要删除 MySQL、Redis、Nacos 的外部数据目录。
