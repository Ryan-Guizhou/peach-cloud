package com.peach.auth.entity;

import com.peach.common.MapperGenerator;
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
@Table(name = "PEACH_AUTH_PARTY")
@Schema(description = "AuthParty实体")
public class AuthPartyDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID")
    @Schema(description = "主键ID")
    private String id;

    @Column(name = "ROLE_CODE")
    @Schema(description = "角色代码")
    private String roleCode;

    @Column(name = "ROLE_TYPE")
    @Schema(description = "角色类型")
    private String roleType;

    @Column(name = "FISCAL")
    @Schema(description = "年度")
    private Integer fiscal;

    @Column(name = "PARTY_CODE")
    @Schema(description = "参与者代码")
    private String partyCode;

    @Column(name = "PARTY_TYPE")
    @Schema(description = "参与者类型")
    private String partyType;

    @Column(name = "CREATE_USER")
    @Schema(description = "创建用户")
    private String createUser;

    @Column(name = "CREATE_TIME")
    @Schema(description = "创建时间")
    private String createTime;

    @Column(name = "LAST_MODIFY_TIME")
    @Schema(description = "最新修改时间")
    private String lastModifyTime;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;

    public static void main(String[] args) {
        System.out.println(MapperGenerator.genMapper(AuthPartyDO.class));
    }

}