package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 替换角色动作在受控资源字段目录上的读写策略。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:45
 */
@Data
@Schema(description = "角色动作字段读写授权策略")
public class SaasFieldPolicyUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 11820261118003L;

    @NotBlank(message = "资源类型不能为空")
    @Pattern(regexp = "^[A-Z][A-Z0-9_.-]{0,63}$", message = "资源类型格式非法")
    private String resourceCode;

    @Valid
    @NotNull(message = "字段策略列表不能为空")
    @Size(max = 128, message = "单次最多设置128项字段策略")
    private List<Rule> policies;

    /**
     * 单个受控字段的读写效果；写入方不得指定脱敏算法。
     */
    @Data
    public static class Rule implements Serializable {

        @Serial
        private static final long serialVersionUID = 11820261118004L;

        @NotBlank(message = "字段物理ID不能为空")
        private String fieldRefId;

        @NotBlank(message = "查看效果不能为空")
        private String readEffect;

        @NotBlank(message = "更新效果不能为空")
        private String writeEffect;
    }
}
