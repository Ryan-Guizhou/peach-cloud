package com.peach.fileservice.dto;

import com.peach.common.PeachGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 云存储实例保存接口参数.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@Schema(description = "云存储实例保存接口参数")
public class CloudStorageInstanceSaveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "存储实例ID")
    @NotBlank(groups = PeachGroup.updateGroup.class, message = "存储实例ID不能为空")
    private String instanceId;

    @Schema(description = "存储实例名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(groups = {PeachGroup.insertGroup.class, PeachGroup.updateGroup.class}, message = "存储实例名称不能为空")
    private String instanceName;

    @Schema(description = "存储类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(groups = {PeachGroup.insertGroup.class, PeachGroup.updateGroup.class}, message = "存储类型不能为空")
    private String storeType;

    @Schema(description = "访问节点地址")
    private String endpoint;
    @Schema(description = "区域")
    private String region;
    @Schema(description = "存储桶名称")
    private String bucketName;
    @Schema(description = "逻辑路径前缀")
    private String prefix;
    @Schema(description = "访问密钥")
    private String accessKey;
    @Schema(description = "加密后的私密密钥")
    private String secretKey;
    @Schema(description = "本地存储或NAS存储根路径")
    private String rootPath;
    @Schema(description = "公共访问域名")
    private String domain;
    @Schema(description = "是否启用Path Style访问，0否1是")
    private Integer pathStyleAccess;
    @Schema(description = "是否允许公共读取，0否1是")
    private Integer publicRead;
    @Schema(description = "服务商扩展配置JSON")
    private String extraJson;
    @Schema(description = "是否启用，0禁用1启用")
    private Integer enabled;
    @Schema(description = "是否内置，0否1是")
    private Integer builtIn;
    @Schema(description = "备注")
    private String remark;
}
