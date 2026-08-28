package com.peach.setting.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.setting.entity.SiteMessageDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 站内信视图对象。
 *
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

    @Serial
    private static final long serialVersionUID = -8087512995309515173L;
}

