package com.peach.setting.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.setting.entity.ValueSetDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:35
 * @Description 值集VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "值集视图对象")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValueSetVO extends ValueSetDO implements Serializable {

    private static final long serialVersionUID = 1L;
}

