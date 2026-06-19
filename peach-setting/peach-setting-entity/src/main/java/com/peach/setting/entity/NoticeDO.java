package com.peach.setting.entity;

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
 * @Description 通知公告实体类
 */
@Data
@Entity
@Table(name = "PEACH_NOTICE")
@Schema(description = "通知公告")
@EqualsAndHashCode(callSuper = true)
public class NoticeDO extends BaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "通知ID")
    private String id;

    @Column(name = "NOTICE_CODE")
    @Schema(description = "通知编码")
    private String noticeCode;

    @Column(name = "TITLE_MESSAGE_KEY")
    @Schema(description = "标题消息Key")
    private String titleMessageKey;

    @Column(name = "CONTENT_MESSAGE_KEY")
    @Schema(description = "内容消息Key")
    private String contentMessageKey;

    @Column(name = "NOTICE_TYPE")
    @Schema(description = "通知类型")
    private String noticeType;

    @Column(name = "PRIORITY")
    @Schema(description = "优先级")
    private Integer priority;

    @Column(name = "PUBLISH_STATUS")
    @Schema(description = "发布状态：DRAFT-草稿, PUBLISHED-已发布, REVOKED-已撤回, OFFLINE-已下线")
    private String publishStatus;

    @Column(name = "EFFECTIVE_FROM")
    @Schema(description = "生效开始时间")
    private String effectiveFrom;

    @Column(name = "EFFECTIVE_TO")
    @Schema(description = "生效结束时间")
    private String effectiveTo;

    @Column(name = "READ_COUNT")
    @Schema(description = "阅读次数")
    private Integer readCount;

    @Column(name = "INBOX_ENABLED")
    @Schema(description = "是否启用收件箱")
    private Integer inboxEnabled;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private Integer status;
}
