package com.peach.auth.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:14
 */
@Data
@Schema(description = "菜单查询参数")
public class MenuQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "菜单ID")
    private String menuId;

    @Schema(description = "菜单编码")
    private String menuCode;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "是否删除 1是 0否")
    private Integer isDelete;

    @Schema(description = "菜单ID集合")
    private List<String> menuIdList;

}
