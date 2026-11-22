package com.peach.auth.dto;

import com.peach.common.PeachGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 路由维护参数；所属租户、应用由服务端确定。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@Data
@Schema(description = "路由维护")
public class RouterDTO {
    @NotBlank(groups = PeachGroup.UpdateGroup.class)
    @Size(max = 64, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String routerId;
    @NotBlank(groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Size(max = 100, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String routerCode;
    @NotBlank(groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Size(max = 100, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String routerName;
    @NotBlank(groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Pattern(regexp = "/[A-Za-z0-9_/-]*", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Size(max = 100, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String routerUrl;
    @NotBlank(groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Pattern(regexp = "[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*", groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Size(max = 255, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String filePath;
    @Min(value = 0, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Max(value = 1, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private Integer isAuth;
    @Min(value = 0, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Max(value = 1, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private Integer isCache;
    @NotBlank(groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Size(max = 10, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String moduleCode;
    @Min(value = 1, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    @Max(value = 20, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private Integer routerLevel;
    @Size(max = 128, groups = {PeachGroup.InsertGroup.class, PeachGroup.UpdateGroup.class})
    private String funcCode;
}
