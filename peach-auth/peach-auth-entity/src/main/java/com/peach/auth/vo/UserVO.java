package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.UserDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 16:44
 */
@Data
@Schema(description = "用户返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVO extends UserDO implements Serializable {

    private static final long serialVersionUID = -3741124344646847872L;

}
