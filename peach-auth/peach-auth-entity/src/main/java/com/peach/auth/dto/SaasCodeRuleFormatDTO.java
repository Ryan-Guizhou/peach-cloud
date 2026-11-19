package com.peach.auth.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 仅允许尚未发号的既有规则变更分隔符和数字宽度。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 10:15
 */
@Data
public class SaasCodeRuleFormatDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 221004L;

    @NotBlank(message = "分隔符不能为空")
    private String separator;

    @NotNull(message = "编号宽度不能为空")
    @Min(value = 1, message = "编号宽度不能小于1")
    @Max(value = 18, message = "编号宽度不能大于18")
    private Integer width;

    @NotNull(message = "预期版本不能为空")
    @Min(value = 0, message = "预期版本不能为负数")
    private Long expectedVersion;
}
