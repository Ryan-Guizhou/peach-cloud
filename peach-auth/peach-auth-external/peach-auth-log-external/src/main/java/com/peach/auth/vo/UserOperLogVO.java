package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.UserOperLogDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户返回视图。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 18:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserOperLogVO extends UserOperLogDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6397634061516958579L;

}
