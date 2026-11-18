package com.peach.gateway.core.security;

/**
 * 网关本次请求从 Sa-Token 单个快照中获取的物理 SaaS 身份及版本。
 *
 * <p>机构可为空；不包含按用户业务号与 FISCAL 推断的旧鉴权语义。
 * 网关缓存只有在服务端验证授权版本后才具备可信性。</p>
 *
 * @param userRefId 全局用户物理主键
 * @param tenantRefId 当前租户物理主键
 * @param memberRefId 当前租户成员物理主键
 * @param appRefId 当前应用物理主键
 * @param memberVersion 成员生命周期版本
 * @param authVersion 成员授权版本
 * @param orgRefId 可选的机构物理主键
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 11:40
 */
public record GatewayTokenContext(String userRefId, String tenantRefId, String memberRefId,
                                  String appRefId, long memberVersion,
                                  long authVersion, String orgRefId) {
}
