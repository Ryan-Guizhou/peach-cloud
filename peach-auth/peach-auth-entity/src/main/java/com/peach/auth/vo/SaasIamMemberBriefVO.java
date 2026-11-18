package com.peach.auth.vo;

import lombok.Data;

/**
 * 同租户且仍有效的最小成员目录项，不包含姓名、手机号及密码摘要。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:50
 */
@Data
public class SaasIamMemberBriefVO {

    private String id;

    private String memberId;

    private String userRefId;
}
