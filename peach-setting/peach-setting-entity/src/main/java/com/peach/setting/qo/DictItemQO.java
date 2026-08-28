package com.peach.setting.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 字典项查询对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:40
 * @Description 字典项查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典项查询对象")
public class DictItemQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 8485179762119338426L;

    @Schema(description = "字典项ID")
    private String id;

    @Schema(description = "字典编码")
    private String dictCode;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "字典项编码")
    private String itemCode;

    @Schema(description = "字典项值")
    private String itemValue;

    @Schema(description = "消息Key")
    private String messageKey;

    @Schema(description = "状态")
    private Integer status;
}


