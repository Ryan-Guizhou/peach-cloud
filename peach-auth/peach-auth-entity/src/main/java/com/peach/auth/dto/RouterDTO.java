package com.peach.auth.dto;

import com.peach.common.PeachGroup;
import jakarta.validation.constraints.NotBlank;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 路由信息。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/18 15:41
 */
@Data
@Schema(description = "路由信息")
public class RouterDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 6818572783834885012L;

    @Schema(description = "物理主键，新增由服务端生成")
    @NotBlank(message = "物理ID不能为空", groups = {PeachGroup.UpdateGroup.class})
    private String id;

    @Schema(description = "路由ID")
    private String routerId;

    @Schema(description = "路由代码")
    private String routerCode;

    @Schema(description = "路由名称")
    private String routerName;

    @Schema(description = "路由路径")
    private String routerUrl;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "是否需要授权")
    private Integer isAuth;

    @Schema(description = "是否缓存")
    private Integer isCache;

    @Schema(description = "模块代码")
    private String moduleCode;

    @Schema(description = "路由级次")
    private Integer routerLevel;
}
