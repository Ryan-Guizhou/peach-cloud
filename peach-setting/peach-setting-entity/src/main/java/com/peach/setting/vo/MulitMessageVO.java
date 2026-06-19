package com.peach.setting.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.setting.entity.MultiMessageDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:35
 * @Description 批量消息VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "批量消息VO")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MulitMessageVO extends MultiMessageDO implements Serializable {

    private static final long serialVersionUID = 1L;
}
