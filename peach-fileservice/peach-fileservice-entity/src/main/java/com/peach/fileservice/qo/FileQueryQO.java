package com.peach.fileservice.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 文件信息查询对象
 *
 * <p>用于文件信息的条件查询，支持按业务类型、业务ID、业务标签、
 * 文件状态等维度进行筛选，同时包含逻辑删除标记以支持软删除查询。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件查询参数")
public class FileQueryQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "业务类型")
    private String bizType;

    @Schema(description = "业务ID")
    private String bizId;

    @Schema(description = "业务标签")
    private String bizTag;

    @Schema(description = "文件状态")
    private String fileStatus;

    @Schema(description = "逻辑删除标记")
    private Integer isDelete;
}
