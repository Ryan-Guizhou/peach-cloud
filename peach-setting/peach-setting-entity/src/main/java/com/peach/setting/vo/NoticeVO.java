package com.peach.setting.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.setting.entity.NoticeDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 通知视图对象。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:35
 * @Description 通知VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "通知视图对象")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoticeVO extends NoticeDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6381103302238970394L;

    @Schema(description = "接收人ID列表")
    private List<String> receiverIdList;
}

