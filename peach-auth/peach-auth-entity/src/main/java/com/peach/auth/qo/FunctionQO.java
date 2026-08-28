package com.peach.auth.qo;

import java.io.Serial;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * Function查询参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Function查询参数")
public class FunctionQO extends PeachEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 6621689789612402150L;

}
