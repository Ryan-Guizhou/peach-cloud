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
 * @Description 公告阅读记录查询对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公告阅读记录查询对象")
public class NoticeReadRecordQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "公告编码")
    private String noticeCode;

    @Schema(description = "阅读用户ID")
    private String readUserId;
}
