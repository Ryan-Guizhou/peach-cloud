package com.peach.setting.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 站内消息。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:40
 * @Description 站内消息查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "站内消息")
public class SiteMessageQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -9185963718739315406L;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    @Schema(description = "消息ID")
    private String id;

    @Schema(description = "消息编码")
    private String messageCode;

    @Schema(description = "接收人ID")
    private String receiverId;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源编码")
    private String sourceCode;

    @Schema(description = "已读标记")
    private Integer readFlag;

    @Schema(description = "发送状态")
    private String sendStatus;
}
