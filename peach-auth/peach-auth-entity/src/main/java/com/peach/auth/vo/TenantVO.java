package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.TenantDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 17:14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "租户返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TenantVO extends TenantDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7766548757472198111L;
}
