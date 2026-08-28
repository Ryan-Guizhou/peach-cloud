package com.peach.auth.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.ResourceDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 资源返回视图。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:41
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "资源返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceVO extends ResourceDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 4225133551183522763L;

}
