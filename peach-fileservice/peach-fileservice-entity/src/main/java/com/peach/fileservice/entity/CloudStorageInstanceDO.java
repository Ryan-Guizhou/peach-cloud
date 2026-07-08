package com.peach.fileservice.entity;

import com.peach.common.PeachDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * 云存储实例实体.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@Entity
@Table(name = "PEACH_STORAGE_INSTANCE")
@Schema(description = "云存储实例实体")
@EqualsAndHashCode(callSuper = true)
public class CloudStorageInstanceDO extends PeachDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "INSTANCE_ID")
    @Schema(description = "存储实例ID")
    private String instanceId;

    @Column(name = "INSTANCE_NAME")
    @Schema(description = "存储实例名称")
    private String instanceName;

    @Column(name = "STORE_TYPE")
    @Schema(description = "存储类型")
    private String storeType;

    @Column(name = "ENDPOINT")
    @Schema(description = "访问节点地址")
    private String endpoint;

    @Column(name = "REGION")
    @Schema(description = "区域")
    private String region;

    @Column(name = "BUCKET_NAME")
    @Schema(description = "存储桶名称")
    private String bucketName;

    @Column(name = "PREFIX")
    @Schema(description = "逻辑路径前缀")
    private String prefix;

    @Column(name = "ACCESS_KEY")
    @Schema(description = "访问密钥")
    private String accessKey;

    @Column(name = "SECRET_KEY")
    @Schema(description = "加密后的私密密钥")
    private String secretKey;

    @Column(name = "ROOT_PATH")
    @Schema(description = "本地存储或NAS存储根路径")
    private String rootPath;

    @Column(name = "DOMAIN")
    @Schema(description = "公共访问域名")
    private String domain;

    @Column(name = "PATH_STYLE_ACCESS")
    @Schema(description = "是否启用Path Style访问，0否1是")
    private Integer pathStyleAccess;

    @Column(name = "PUBLIC_READ")
    @Schema(description = "是否允许公共读取，0否1是")
    private Integer publicRead;

    @Column(name = "EXTRA_JSON")
    @Schema(description = "服务商扩展配置JSON")
    private String extraJson;

    @Column(name = "ENABLED")
    @Schema(description = "是否启用，0禁用1启用")
    private Integer enabled;

    @Column(name = "REMARK")
    @Schema(description = "备注")
    private String remark;

    @Column(name = "BUILT_IN")
    @Schema(description = "是否内置，0否1是")
    private Integer builtIn;
}
