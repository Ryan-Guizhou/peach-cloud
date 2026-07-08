package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.ResourceDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:41
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "资源返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceVO extends ResourceDO implements Serializable {
    private static final long serialVersionUID = 1L;

}
