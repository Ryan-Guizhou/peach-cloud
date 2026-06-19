package com.peach.fileservice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.fileservice.entity.FileObjectDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 文件物理对象视图对象
 *
 * <p>用于向前端展示文件物理存储对象的完整信息，继承自{@link FileObjectDO}，
 * 包含文件摘要（SHA-256、MD5）、文件大小、存储提供方、Bucket、对象Key、
 * 原始文件名、内容类型、扩展名、存储状态、引用计数及上传/访问时间等字段。
 * 通过{@link JsonInclude.Include#NON_NULL}策略序列化时自动忽略空值字段，
 * 确保接口返回数据简洁。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件物理对象视图")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileObjectVO extends FileObjectDO implements Serializable {

    private static final long serialVersionUID = 1L;
}
