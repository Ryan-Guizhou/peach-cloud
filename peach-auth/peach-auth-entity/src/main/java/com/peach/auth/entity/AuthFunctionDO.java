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
@Table(name = "PEACH_AUTH_FUNCTION")
@Schema(description = "AuthFunction实体")
public class AuthFunctionDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "PARTY_CODE")
    @Schema(description = "参与者代码")
    private String partyCode;

    @Column(name = "PARTY_TYPE")
    @Schema(description = "参与者类型，角色编码")
    private String partyType;

    @Column(name = "FUNC_CODE")
    @Schema(description = "功能代码")
    private String funcCode;

    @Column(name = "FISCAL")
    @Schema(description = "年度")
    private Integer fiscal;

    @Column(name = "STATE")
    @Schema(description = "功能状态")
    private String state;

    @Column(name = "APP_ID")
    @Schema(description = "应用ID")
    private String appId;


    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;

    public static void main(String[] args) {
        System.out.println(MapperGenerator.genMapper(AuthFunctionDO.class));
    }

}
