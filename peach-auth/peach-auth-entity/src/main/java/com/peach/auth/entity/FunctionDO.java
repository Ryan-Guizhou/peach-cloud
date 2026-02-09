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
@Table(name = "PEACH_FUNCTION")
@Schema(description = "Function实体")
public class FunctionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "FUNC_ID")
    @Schema(description = "功能ID")
    private String funcId;

    @Column(name = "FUNC_CODE")
    @Schema(description = "功能编码")
    private String funcCode;

    @Column(name = "PARENT_FUNC_CODE")
    @Schema(description = "父功能编码")
    private String parentFuncCode;

    @Column(name = "FUNC_NAME")
    @Schema(description = "功能名称")
    private String funcName;

    @Column(name = "FUNC_DESC")
    @Schema(description = "功能描述")
    private String funcDesc;

    @Column(name = "FUNC_URL")
    @Schema(description = "功能URL")
    private String funcUrl;

    @Column(name = "FUNC_SEQ")
    @Schema(description = "功能序列号")
    private String funcSeq;

    @Column(name = "FUNC_TYPE")
    @Schema(description = "功能类型")
    private String funcType;

    @Column(name = "IS_MENU")
    @Schema(description = "是否菜单")
    private Integer isMenu;

    @Column(name = "IS_AUTHORIZE")
    @Schema(description = "是否访问授权")
    private Integer isAuthorize;

    @Column(name = "APP_ID")
    @Schema(description = "应用ID")
    private String appId;

    @Column(name = "CREATE_TIME")
    @Schema(description = "创建时间")
    private String createTime;

    @Column(name = "IS_DISABLE")
    @Schema(description = "是否禁用")
    private Integer isDisable;

    @Column(name = "LAST_MODIFY_TIME")
    @Schema(description = "最新修改时间")
    private String lastModifyTime;

    @Column(name = "IS_DELETE")
    @Schema(description = "是否删除")
    private Integer isDelete;

    public static void main(String[] args) {
        System.out.println(MapperGenerator.genMapper(FunctionDO.class));
    }

}