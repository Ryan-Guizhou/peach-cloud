package com.peach.message.entity;

import com.peach.common.BaseDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:30
 * @Description 站内信消息
 */
@Data
@Entity
@Table(name = "PEACH_SITE_MESSAGE")
@Schema(description = "站内信消息")
@EqualsAndHashCode(callSuper = true)
public class SiteMessageDO extends BaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "消息ID")
    private String id;

    @Column(name = "MESSAGE_CODE")
    @Schema(description = "消息编码")
    private String messageCode;

    @Column(name = "RECEIVER_ID")
    @Schema(description = "接收人用户ID")
    private String receiverId;

    @Column(name = "TITLE_MESSAGE_KEY")
    @Schema(description = "标题消息Key")
    private String titleMessageKey;

    @Column(name = "CONTENT_MESSAGE_KEY")
    @Schema(description = "内容消息Key")
    private String contentMessageKey;

    @Column(name = "MESSAGE_TYPE")
    @Schema(description = "消息类型，小类")
    private String messageType;

    @Column(name = "SOURCE_TYPE")
    @Schema(description = "来源类型")
    private String sourceType;

    @Column(name = "SOURCE_CODE")
    @Schema(description = "来源编码")
    private String sourceCode;

    @Column(name = "READ_FLAG")
    @Schema(description = "已读标记")
    private Integer readFlag;

    @Column(name = "SEND_STATUS")
    @Schema(description = "发送状态：DRAFT草稿、SENT已发送、REVOKED已撤回")
    private String sendStatus;
}
