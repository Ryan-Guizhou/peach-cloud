package com.peach.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 编码规则启用或停用的乐观版本参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 10:15
 */
@Data
public class SaasCodeRuleStatusDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 221003L;

    @NotNull(message = "规则状态不能为空")
    private Boolean enabled;

    @NotNull(message = "预期版本不能为空")
    @Min(value = 0, message = "预期版本不能为负数")
    private Long expectedVersion;
}
