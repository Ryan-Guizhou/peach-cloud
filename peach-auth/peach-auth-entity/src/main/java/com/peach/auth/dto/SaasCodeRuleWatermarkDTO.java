package com.peach.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 经受信管理员批准且只能向前推进的编码水位调整参数。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19 10:15
 */
@Data
public class SaasCodeRuleWatermarkDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 221005L;

    @NotNull(message = "目标水位不能为空")
    @Min(value = 0, message = "目标水位不能为负数")
    private Long watermark;

    @NotNull(message = "预期版本不能为空")
    @Min(value = 0, message = "预期版本不能为负数")
    private Long expectedVersion;
}
