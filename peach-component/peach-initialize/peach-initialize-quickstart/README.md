# Peach Initialize Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-initialize-starter` 的最小接入。实际初始化处理器应注册为业务 Bean，并保持可控超时、幂等和启动失败语义。

```bash
mvn -f peach-component/peach-initialize/peach-initialize-quickstart/pom.xml spring-boot:run -Pdevelopment
```

初始化处理器契约与边界见父级 [`README.md`](../README.md)。
