package com.peach.setting.entity;

import com.peach.common.PeachDO;
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
 * @Description 多语言消息 */

@Data
@Entity
@Table(name = "PEACH_MULTI_MESSAGE")
@Schema(description = "多语言消息")
@EqualsAndHashCode(callSuper = true)
public class MultiMessageDO extends PeachDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "MESSAGE_KEY")
    @Schema(description = "消息键")
    private String messageKey;

    @Column(name = "LOCALE")
    @Schema(description = "语言区域")
    private String locale;

    @Column(name = "MODULE_CODE")
    @Schema(description = "模块编码")
    private String moduleCode;

    @Column(name = "MESSAGE_TYPE")
    @Schema(description = "消息类型")
    private String messageType;

    @Column(name = "USAGE_SCOPE")
    @Schema(description = "使用范围：COMMON通用、BACKEND后端、FRONTEND前端、BOTH全部")
    private String usageScope;

    @Column(name = "MESSAGE_CONTENT")
    @Schema(description = "消息内容")
    private String messageContent;

    @Column(name = "DESCRIPTION")
    @Schema(description = "描述")
    private String description;

    @Column(name = "STATUS")
    @Schema(description = "状态")
    private Integer status;
}

