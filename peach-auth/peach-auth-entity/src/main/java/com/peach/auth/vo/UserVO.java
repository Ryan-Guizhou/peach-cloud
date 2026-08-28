package com.peach.auth.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.UserDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户返回视图。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVO extends UserDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 9114274293498807264L;

}
