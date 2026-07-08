package com.peach.auth.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.auth.entity.OrganizationDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 机构返回视图。
 * <p>基于机构主数据封装前端展示字段，不承载写入语义。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Data
@Schema(description = "机构返回视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationVO extends OrganizationDO implements Serializable {
    private static final long serialVersionUID = 1L;
}
