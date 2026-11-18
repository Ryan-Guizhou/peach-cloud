package com.peach.auth.vo;

import lombok.Data;

/**
 * 同租户且启用的最小机构目录项，父子关系使用物理主键。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:50
 */
@Data
public class SaasIamOrganizationBriefVO {

    private String id;

    private String orgId;

    private String parentOrgRefId;
}
