# Peach Redisson DelayQueue Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-redission-delayqueue-starter` 的最小接入。消费者必须考虑重复投递、失败重试、幂等与积压治理，不能把队列当成 Exactly-Once。

```bash
mvn -f peach-middleware/peach-redission/peach-redission-delayqueue-quickstart/pom.xml spring-boot:run -Pdevelopment
```
