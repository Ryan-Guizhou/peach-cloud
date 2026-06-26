package com.peach.message.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.message.entity.SiteMessageDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:35
 * @Description 站内信VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "站内信视图对象")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SiteMessageVO extends SiteMessageDO implements Serializable {

    private static final long serialVersionUID = 1L;

}
