package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.MenuDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:05
 */
@Data
@Schema(description = "菜单返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuVO extends MenuDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "已授权资源列表")
    private List<ResourceVO> resourceList;

}
