package com.peach.auth.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "授权部门查询参数")
public class AuthPartyQO extends PeachEntity implements Serializable {
    private static final long serialVersionUID = 1L;
}
