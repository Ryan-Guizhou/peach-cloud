package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.RouterDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:34
 */
@Data
@Schema(description = "路由返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterVO extends RouterDO implements Serializable {
    private static final long serialVersionUID = 1L;
}
