package com.peach.auth.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:08
 */
@Data
@Schema(description = "AuthFunctionQO查询参数")
public class AuthFunctionQO extends PeachEntity implements Serializable {
    private static final long serialVersionUID = 1L;
}
