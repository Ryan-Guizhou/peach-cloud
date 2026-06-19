package com.peach.fileservice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.fileservice.entity.FileRecordDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 业务文件记录视图对象
 *
 * <p>继承自 {@link FileRecordDO}，在基础文件记录之上扩展了存储提供方、
 * Bucket 名称、对象 Key 及存储状态等字段，用于向前端展示完整的文件存储信息。</p>
 *
 * <p>主要特征：</p>
 * <ul>
 *   <li>包含存储提供方、Bucket、对象 Key 等对象存储定位信息</li>
 *   <li>暴露对象存储状态，便于前端判断文件可用性</li>
 *   <li>通过 {@link JsonInclude.Include#NON_NULL} 过滤空值字段，减少传输体积</li>
 * </ul>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "业务文件记录视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileRecordVO extends FileRecordDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "存储提供方")
    private String storageProvider;

    @Schema(description = "bucket名称")
    private String bucketName;

    @Schema(description = "对象key")
    private String objectKey;

    @Schema(description = "对象存储状态")
    private String storageStatus;
}
