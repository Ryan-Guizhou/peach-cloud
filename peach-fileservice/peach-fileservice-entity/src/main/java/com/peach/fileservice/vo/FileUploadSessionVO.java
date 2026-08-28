package com.peach.fileservice.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.fileservice.entity.FileUploadSessionDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 文件上传会话视图。
 * <p>用于向前端返回文件上传会话的完整信息，继承自 {@link FileUploadSessionDO}，
 * 涵盖会话标识、文件摘要（SHA-256 / MD5）、文件元数据（名称、大小、类型）、
 * 业务关联（业务类型 / ID / 标签）、存储位置（Provider / Bucket / ObjectKey）
 * 以及上传状态与过期时间等字段。</p>
 * <p>通过 {@code @JsonInclude(NON_NULL)} 策略，序列化时自动忽略空值字段，
 * 确保接口响应简洁干净，避免冗余的 null 值传输。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件上传会话视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadSessionVO extends FileUploadSessionDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 386142578228039038L;
}
