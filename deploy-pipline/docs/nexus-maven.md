# Maven 与 Nexus

CI Maven 的唯一仓库入口是 `http://nexus:8081/repository/maven-public/`。[`settings.xml`](../config/maven/settings.xml) 使用 `<mirrorOf>*</mirrorOf>`，因此 Maven 客户端不直接访问 Aliyun/Central；上游代理由 Nexus `maven-public` group 管理。

根 `pom.xml` 已配置 `peach-releases` 与 `peach-snapshots` distributionManagement。流水线执行 `mvn ... clean deploy`：Snapshot 上传 `maven-snapshots`，Release 上传 `maven-releases`。

Nexus 认证来自 Jenkins Secret file 中 `MAVEN_NEXUS_USERNAME/MAVEN_NEXUS_PASSWORD`，仓库只保存 settings 模板。
