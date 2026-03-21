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
@Table(name = "PEACH_ROUTER")
@Schema(description = "Router实体")
public class RouterDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ROUTER_ID")
    @Schema(description = "主键")
    private String routerId;

    @Column(name = "ROUTER_CODE")
    @Schema(description = "路由代码")
    private String routerCode;

    @Column(name = "ROUTER_NAME")
    @Schema(description = "路由名称")
    private String routerName;

    @Column(name = "ROUTER_URL")
    @Schema(description = "路由路径")
    private String routerUrl;

    @Column(name = "FILE_PATH")
    @Schema(description = "文件路径")
    private String filePath;

    @Column(name = "IS_AUTH")
    @Schema(description = "是否需要授权")
    private Integer isAuth;

    @Column(name = "IS_CACHE")
    @Schema(description = "是否缓存")
    private Integer isCache;

    @Column(name = "MODULE_CODE")
    @Schema(description = "模块代码")
    private String moduleCode;

    @Column(name = "ROUTER_LEVEL")
    @Schema(description = "路由级次")
    private Integer routerLevel;

    public static void main(String[] args) {
        System.out.println(MapperGenerator.genMapper(RouterDO.class));
    }

}