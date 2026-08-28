package com.peach.setting.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 通知公告查询对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:40
 * @Description 通知公告查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通知公告查询对象")
public class NoticeQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -8327979212221967767L;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "通知ID")
    private String id;

    @Schema(description = "通知编码")
    private String noticeCode;

    @Schema(description = "通知类型")
    private String noticeType;

    @Schema(description = "发布状态")
    private String publishStatus;

    @Schema(description = "是否启用站内信")
    private Integer inboxEnabled;

    @Schema(description = "状态")
    private Integer status;
}
