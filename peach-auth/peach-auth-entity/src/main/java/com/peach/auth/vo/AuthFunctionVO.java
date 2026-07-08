package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.AuthFunctionDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:39
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "权限功能返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthFunctionVO extends AuthFunctionDO implements Serializable {
    private static final long serialVersionUID = -3741124344646847872L;
}
