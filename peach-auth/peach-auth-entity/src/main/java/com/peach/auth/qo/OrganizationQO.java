package com.peach.auth.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 机构查询条件。
 * <p>用于机构列表、分页和详情查询，默认按租户和机构状态过滤。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Data
@Schema(description = "机构查询参数")
public class OrganizationQO extends PeachEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构编码")
    private String orgCode;

    @Schema(description = "机构名称")
    private String orgName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否删除")
    private Integer isDelete;
}
