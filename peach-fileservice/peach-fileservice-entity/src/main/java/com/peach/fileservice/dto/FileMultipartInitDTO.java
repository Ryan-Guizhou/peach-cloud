package com.peach.fileservice.dto;

import java.io.Serial;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 分片上传初始化参数。
 * <p>用于大文件分片上传场景的初始化请求，继承 {@link FileUploadCheckDTO} 的全部校验字段
 * （包括文件SHA256、MD5、文件大小、文件名、内容类型、业务类型等），
 * 作为分片上传初始化接口的入参载体，触发服务端创建分片上传任务并返回分片策略。</p>
 * <p>主要特征：</p>
 * <ul>
 * <li>复用文件上传校验参数，无需重复定义字段</li>
 * <li>支持秒传检测：通过SHA256判断文件是否已存在，已存在则跳过分片上传</li>
 * <li>支持业务关联：通过bizType、bizId、bizTag将上传文件与业务数据绑定</li>
 * <li>支持指定存储提供方：通过storageProvider字段选择目标存储服务</li>
 * </ul>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "分片上传初始化参数")
public class FileMultipartInitDTO extends FileUploadCheckDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1155320714655821256L;
}
