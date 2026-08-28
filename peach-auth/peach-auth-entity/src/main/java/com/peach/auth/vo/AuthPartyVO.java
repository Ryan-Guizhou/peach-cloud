package com.peach.auth.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.AuthPartyDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * AuthParty返回视图。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:39
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "AuthParty返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthPartyVO extends AuthPartyDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 2725986338896008272L;
}
