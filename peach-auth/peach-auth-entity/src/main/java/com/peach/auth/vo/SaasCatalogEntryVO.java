package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SaaS 管理目录的最小列表行，ID 始终为对应实体的物理主键。
 *
 * <p>仅包含角色、成员、机构和权限选择所需元数据，不暴露用户密码、电话、
 * 邮箱、证件号，也不接受客户端传回的租户身份作为查询过滤条件。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:30
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "SaaS受信目录行")
public class SaasCatalogEntryVO {

    @Schema(description = "实体物理主键")
    private String id;

    @Schema(description = "稳定业务标识")
    private String code;

    @Schema(description = "展示名称")
    private String name;

    @Schema(description = "权限资源类型")
    private String resourceType;

    @Schema(description = "绑定中的角色物理主键")
    private String roleRefId;

    @Schema(description = "独立绑定的范围类型")
    private String scopeType;

    @Schema(description = "ORG或ORG_TREE作用域的机构物理主键")
    private String scopeRefId;
}
