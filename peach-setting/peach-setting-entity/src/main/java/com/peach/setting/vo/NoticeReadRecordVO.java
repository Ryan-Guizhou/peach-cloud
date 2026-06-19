package com.peach.setting.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.setting.entity.NoticeReadRecordDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:35
 * @Description 通知阅读记录VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通知阅读记录视图对象")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeReadRecordVO extends NoticeReadRecordDO implements Serializable {

    private static final long serialVersionUID = 1L;
}

