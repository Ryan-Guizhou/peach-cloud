package com.peach.auth.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "路由查询参数")
public class RouterQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "路由编码")
    private String routerCode;

    @Schema(description = "路由名称")
    private String routerName;

    @Schema(description = "模块编码")
    private String moduleCode;

}
