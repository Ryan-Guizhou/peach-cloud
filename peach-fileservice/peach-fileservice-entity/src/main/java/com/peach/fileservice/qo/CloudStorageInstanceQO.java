package com.peach.fileservice.qo;

import com.peach.common.PeachEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 云存储实例查询参数.
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/9 00:01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "云存储实例查询参数")
public class CloudStorageInstanceQO extends PeachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "租户ID")
    private String tenantId;

    @Schema(description = "机构ID")
    private String orgId;

    /**
     * 存储实例名称。
     *
     * <p>
     * 支持模糊查询，用于根据实例名称筛选存储配置。
     * </p>
     */
    @Schema(description = "存储实例名称")
    private String instanceName;

    /**
     * 存储类型。
     *
     * <p>
     * 用于按照存储类型进行过滤，
     * 例如：
     * OSS、OBS、COS、S3、NAS 等。
     * </p>
     */
    @Schema(description = "存储类型")
    private String storeType;

    /**
     * 启用状态。
     *
     * <p>
     * 用于筛选存储实例是否启用：
     * </p>
     *
     * <ul>
     *     <li>1：启用</li>
     *     <li>0：禁用</li>
     * </ul>
     */
    @Schema(description = "启用状态，1启用，0禁用")
    private Integer enabled;
}
