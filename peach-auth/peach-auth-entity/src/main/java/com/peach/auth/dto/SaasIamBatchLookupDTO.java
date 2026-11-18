package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 批量最小目录查询；不接受租户、应用、用户或年度字段。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 12:50
 */
@Data
@Schema(description = "可信服务内按物理主键批量查询")
public class SaasIamBatchLookupDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 202611180904L;

    @NotNull(message = "物理ID列表不能为空")
    @Size(max = 100, message = "单次最多查询100条")
    private List<String> ids;
}
