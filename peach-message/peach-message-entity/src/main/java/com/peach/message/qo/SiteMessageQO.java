package com.peach.message.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:40
 * @Description 站内消息查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "站内消息")
public class SiteMessageQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    private String id;

    @Schema(description = "消息编码")
    private String messageCode;

    @Schema(description = "接收人ID")
    private String receiverId;

    @Schema(description = "消息类型")
    private String messageType;

    @Schema(description = "消息类型列表")
    private java.util.List<String> messageTypeList;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源编码")
    private String sourceCode;

    @Schema(description = "已读标记")
    private Integer readFlag;

    @Schema(description = "发送状态")
    private String sendStatus;

    @Schema(description = "信息")
    private String titleMessageKey;


}
