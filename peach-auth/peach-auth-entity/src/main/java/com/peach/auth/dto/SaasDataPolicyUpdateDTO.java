package com.peach.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 替换角色某个动作的全部行级策略，成员、租户及应用从 Token 获取。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:45
 */
@Data
@Schema(description = "角色动作行级数据授权策略")
public class SaasDataPolicyUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 11820261118001L;

    @Valid
    @NotNull(message = "策略列表不能为空")
    @Size(max = 32, message = "单次最多设置32项数据策略")
    private List<Rule> policies;

    /**
     * 一个可独立核验的行级授权范围。
     */
    @Data
    public static class Rule implements Serializable {

        @Serial
        private static final long serialVersionUID = 11820261118002L;

        @NotBlank(message = "数据范围不能为空")
        private String dataScope;

        private String orgRefId;
    }
}
