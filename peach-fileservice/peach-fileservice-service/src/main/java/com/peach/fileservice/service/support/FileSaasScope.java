package com.peach.fileservice.service.support;

import org.springframework.util.StringUtils;

/**
 * Auth 在线核验后得到的单次请求物理范围。
 *
 * <p>所有范围由服务端根据当前最终用户 Token 获取，不可通过 HTTP 参数构造。
 * 当前文件记录默认仅对所有者可见；机构树/角色范围及字段级授权未完成时继续拒绝，
 * 绝不能降级为旧业务编号范围。</p>
 *
 * @param tenantRefId 物理租户 UUID
 * @param appRefId 物理应用 UUID
 * @param userRefId 所有者物理 UUID
 * @param memberRefId 成员物理 UUID
 * @param tenantId 永久业务租户编号，仅兼容旧审计字段
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
public record FileSaasScope(String tenantRefId, String appRefId,
                            String userRefId, String memberRefId, String tenantId) {

    public FileSaasScope {
        if (!StringUtils.hasText(tenantRefId) || !StringUtils.hasText(appRefId)
                || !StringUtils.hasText(userRefId) || !StringUtils.hasText(memberRefId)
                || !StringUtils.hasText(tenantId)) {
            throw new SecurityException("Valid trusted file SaaS scope is required");
        }
    }
}
