package com.peach.setting.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:40
 * @Description 值集查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "值集查询对象")
public class ValueSetQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "值集编码")
    private String valueSetCode;

    @Schema(description = "值集名称")
    private String valueSetName;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "状态")
    private Integer status;
}
