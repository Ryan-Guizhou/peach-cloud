# Peach Redisson DistributedLock Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-redission-distributedlock-starter` 的最小接入。锁只保护最小临界区，不替代数据库唯一约束、事务或业务状态校验。

```bash
mvn -f peach-middleware/peach-redission/peach-redission-distributedlock-quickstart/pom.xml spring-boot:run -Pdevelopment
```
