package com.peach.auth.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.RoleDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 角色返回视图。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:40
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleVO extends RoleDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 3236435610883536732L;

}
