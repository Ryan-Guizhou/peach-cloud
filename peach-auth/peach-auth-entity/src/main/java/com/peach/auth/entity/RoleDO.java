package com.peach.auth.entity;

import com.peach.common.MapperGenerator;
import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 17:36
 */
@Data
@Entity
@Table(name = "PEACH_ROLE")
@Schema(description = "Role实体")
public class RoleDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ROLE_ID")
    @Schema(description = "角色ID")
    private String roleId;

    @Column(name = "ROLE_CODE")
    @Schema(description = "角色编码")
    private String roleCode;

    @Column(name = "FISCAL")
    @Schema(description = "年度")
    private Integer fiscal;

    @Column(name = "ROLE_NAME")
    @Schema(description = "角色名称")
    private String roleName;

    @Column(name = "ROLE_DESC")
    @Schema(description = "角色描述")
    private String roleDesc;

    @Column(name = "ROLE_SCOPE")
    @Schema(description = "角色范围")
    private String roleScope;

    @Column(name = "ROLE_TYPE")
    @Schema(description = "角色类型")
    private String roleType;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;

    @Column(name = "SKIP_URL")
    @Schema(description = "角色登陆跳转")
    private String skipUrl;

    public static void main(String[] args) {
        System.out.println(MapperGenerator.genMapper(RoleDO.class));
    }

}