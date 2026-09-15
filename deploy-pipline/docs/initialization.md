# 数据初始化

## MySQL

[`init-mysql.sh`](../scripts/init/init-mysql.sh) 只在目标 database 表数量为 0 时执行 baseline schema 和 seed。已存在任何表时整个 baseline 初始化跳过，以保护现有数据库。baseline 来源于仓库已有 `ALL_TABLE_CREATE.sql` 和 `INIT.sql` 的部署副本。

## MongoDB

[`init-mongodb.sh`](../scripts/init/init-mongodb.sh) 确保业务 database/user 存在；已有用户保留，不自动改密码或删除 collection。`init/mongodb/schema`、`indexes`、`data` 下可放幂等 `*.js`。

## Nacos

[`init-nacos.sh`](../scripts/init/init-nacos.sh) 默认只创建缺失 namespace/config；已有 Data ID 保留。只有人工显式执行 `init-nacos.sh --sync` 才把仓库模板同步覆盖到 Nacos。
